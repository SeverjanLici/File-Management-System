import { useEffect, useRef, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  useAddDocumentVersion,
  useDocument,
  useRemoveDocumentShare,
  useShareDocument,
  useDeleteDocument,
  useUpdateDocument,
} from "../hooks/useDocuments";
import { useProcessingResult, useProcessingStatus } from "../hooks/useProcessing";
import { useApi } from "../hooks/useApi";
import { endpoints } from "../config/api";
import FileUploader from "../components/FileUploader";
import ShareDialog from "../components/ShareDialog";
import Loading from "../components/Loading";
import { Badge } from "../components/ui/badge";
import { Button } from "../components/ui/button";
import type {
  CreateShareRequest,
  DocumentVersion,
  PresignedUrl,
  UpdateDocumentRequest,
} from "../types";
import { Card, CardContent } from "@/components/ui/card";

interface UploadedFile {
  fileId: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
}

interface DocumentMetadataForm {
  name: string;
  description: string;
  category: string;
  tags: string;
  summary: string;
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return "0 Bytes";
  const k = 1024;
  const sizes = ["Bytes", "KB", "MB", "GB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
}

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleString();
}

function isPreviewableMimeType(mimeType: string): boolean {
  return (
    mimeType.startsWith("image/") ||
    mimeType.startsWith("video/") ||
    mimeType.startsWith("audio/") ||
    mimeType === "application/pdf" ||
    mimeType.startsWith("text/") ||
    mimeType === "application/json"
  );
}

function getPreviewKind(
  mimeType: string,
): "image" | "video" | "audio" | "document" | "unsupported" {
  if (mimeType.startsWith("image/")) return "image";
  if (mimeType.startsWith("video/")) return "video";
  if (mimeType.startsWith("audio/")) return "audio";
  if (
    mimeType === "application/pdf" ||
    mimeType.startsWith("text/") ||
    mimeType === "application/json"
  ) {
    return "document";
  }
  return "unsupported";
}

function toTagArray(tags: string): string[] {
  return Array.from(
    new Set(
      tags
        .split(",")
        .map((tag) => tag.trim())
        .filter(Boolean),
    ),
  );
}

function toMetadataForm(document: {
  name: string;
  description?: string;
  category?: string;
  tags: string[];
  summary?: string;
}): DocumentMetadataForm {
  return {
    name: document.name,
    description: document.description ?? "",
    category: document.category ?? "",
    tags: document.tags.join(", "),
    summary: document.summary ?? "",
  };
}

function DocumentDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: document, isLoading, error } = useDocument(id!);
  const { data: processingStatus } = useProcessingStatus(document?.fileId);
  const { data: processingResult } = useProcessingResult(document?.fileId);
  const addDocumentVersion = useAddDocumentVersion();
  const shareDocument = useShareDocument();
  const removeDocumentShare = useRemoveDocumentShare();
  const deleteDocument = useDeleteDocument();
  const updateDocument = useUpdateDocument();
  const { accessToken } = useApi();
  const [showShareDialog, setShowShareDialog] = useState(false);
  const [showVersionUploader, setShowVersionUploader] = useState(false);
  const [isEditingMetadata, setIsEditingMetadata] = useState(false);
  const [metadataError, setMetadataError] = useState("");
  const [versionError, setVersionError] = useState("");
  const [selectedPreviewVersionId, setSelectedPreviewVersionId] = useState<
    string | null
  >(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [previewError, setPreviewError] = useState("");
  const [isPreviewLoading, setIsPreviewLoading] = useState(false);
  const versionUploaderRef = useRef<HTMLDivElement | null>(null);
  const [metadataForm, setMetadataForm] = useState<DocumentMetadataForm>({
    name: "",
    description: "",
    category: "",
    tags: "",
    summary: "",
  });

  useEffect(() => {
    if (!document) return;

    const currentVersion = document.versions.find(
      (version) => version.versionNumber === document.currentVersionNumber,
    );

    setSelectedPreviewVersionId(
      currentVersion?.id || document.versions[0]?.id || null,
    );
    setMetadataForm(toMetadataForm(document));
  }, [document]);

  const selectedPreviewVersion = document?.versions.find(
    (version) => version.id === selectedPreviewVersionId,
  );

  useEffect(() => {
    async function loadPreview() {
      if (!selectedPreviewVersion) {
        setPreviewUrl(null);
        setPreviewError("");
        return;
      }

      if (!isPreviewableMimeType(selectedPreviewVersion.mimeType)) {
        setPreviewUrl(null);
        setPreviewError("");
        return;
      }

      setIsPreviewLoading(true);
      setPreviewError("");

      try {
        const response = await fetch(
          endpoints.files.preview(selectedPreviewVersion.fileId),
          {
            headers: { Authorization: `Bearer ${accessToken}` },
          },
        );

        if (!response.ok) {
          throw new Error("Failed to load preview URL");
        }

        const body = (await response.json()) as { data?: PresignedUrl };
        if (!body.data?.url) {
          throw new Error("Preview URL missing");
        }

        setPreviewUrl(body.data.url);
      } catch (previewLoadError) {
        console.error("Preview failed", previewLoadError);
        setPreviewUrl(null);
        setPreviewError("Preview could not be loaded for this file.");
      } finally {
        setIsPreviewLoading(false);
      }
    }

    void loadPreview();
  }, [accessToken, selectedPreviewVersion]);

  useEffect(() => {
    if (!showVersionUploader) {
      return;
    }

    versionUploaderRef.current?.scrollIntoView({
      behavior: "smooth",
      block: "start",
    });
  }, [showVersionUploader]);

  if (isLoading) return <Loading />;

  if (error || !document) {
    return (
      <div className="text-center py-8">
        <p className="text-red-600">Document not found</p>
        <Button
          onClick={() => navigate("/documents")}
          variant="link"
          className="mt-4"
        >
          Back to documents
        </Button>
      </div>
    );
  }

  const handleDownload = async (fileId: string, downloadName: string) => {
    try {
      const res = await fetch(endpoints.files.presigned(fileId), {
        headers: { Authorization: `Bearer ${accessToken}` },
      });

      if (!res.ok) {
        const errText = await res.text();
        console.error("Presigned URL request failed", res.status, errText);
        window.alert("Failed to get download URL");
        return;
      }

      const body = await res.json();
      const url = body?.data?.url;
      if (!url) {
        console.error("No presigned URL in response", body);
        window.alert("Failed to get download URL");
        return;
      }

      // Download it!
      const a = window.document.createElement("a");
      a.href = url;
      a.download = downloadName || "download";
      window.document.body.appendChild(a);
      a.click();
      window.document.body.removeChild(a);
    } catch (e) {
      console.error("Download failed", e);
      window.alert("Download failed");
    }
  };

  const handleShare = async (data: CreateShareRequest) => {
    await shareDocument.mutateAsync({ documentId: document.id, data });
  };

  const handleRemoveShare = async (shareId: string) => {
    await removeDocumentShare.mutateAsync({ documentId: document.id, shareId });
  };

  const handleDelete = async () => {
    if (window.confirm("Are you sure you want to delete this document?")) {
      await deleteDocument.mutateAsync(document.id);
      navigate("/documents");
    }
  };

  const handleVersionUpload = async (fileInfo: UploadedFile) => {
    setVersionError("");

    try {
      await addDocumentVersion.mutateAsync({
        documentId: document.id,
        data: fileInfo,
      });
      setShowVersionUploader(false);
    } catch (err) {
      setVersionError("Failed to create the new document version");
    }
  };

  const handleMetadataChange = (
    field: keyof DocumentMetadataForm,
    value: string,
  ) => {
    setMetadataForm((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const handleMetadataSave = async () => {
    const trimmedName = metadataForm.name.trim();

    if (!trimmedName) {
      setMetadataError("Name is required.");
      return;
    }

    setMetadataError("");

    const payload: UpdateDocumentRequest = {
      name: trimmedName,
      description: metadataForm.description.trim(),
      category: metadataForm.category.trim(),
      tags: toTagArray(metadataForm.tags),
      summary: metadataForm.summary.trim(),
    };

    try {
      await updateDocument.mutateAsync({
        documentId: document.id,
        data: payload,
      });
      setIsEditingMetadata(false);
    } catch (saveError) {
      setMetadataError(
        saveError instanceof Error
          ? saveError.message
          : "Failed to update document details.",
      );
    }
  };

  const handleMetadataCancel = () => {
    setMetadataForm(toMetadataForm(document));
    setMetadataError("");
    setIsEditingMetadata(false);
  };

  const canEdit = document.permissions.includes("EDIT");
  const canDelete = document.permissions.includes("DELETE");
  const canShare = document.permissions.includes("SHARE");
  const shareError = shareDocument.error || removeDocumentShare.error;
  const isSharingBusy =
    shareDocument.isPending || removeDocumentShare.isPending;
  const previewKind = selectedPreviewVersion
    ? getPreviewKind(selectedPreviewVersion.mimeType)
    : "unsupported";

  const renderPreview = (version: DocumentVersion) => {
    if (!isPreviewableMimeType(version.mimeType)) {
      return (
        <div className="rounded-md border border-dashed border-gray-300 bg-gray-50 p-6 text-sm text-gray-600">
          Preview is not available for <strong>{version.mimeType}</strong>. Use
          download instead.
        </div>
      );
    }

    if (isPreviewLoading) {
      return (
        <div className="rounded-md border border-gray-200 bg-gray-50 p-6 text-sm text-gray-600">
          Loading preview...
        </div>
      );
    }

    if (previewError) {
      return (
        <div className="rounded-md border border-red-200 bg-red-50 p-6 text-sm text-red-700">
          {previewError}
        </div>
      );
    }

    if (!previewUrl) {
      return (
        <div className="rounded-md border border-gray-200 bg-gray-50 p-6 text-sm text-gray-600">
          Preview URL unavailable.
        </div>
      );
    }

    if (previewKind === "image") {
      return (
        <div className="overflow-hidden rounded-md border border-gray-200 bg-gray-100">
          <img
            src={previewUrl}
            alt={`Preview of ${version.fileName}`}
            className="max-h-[36rem] w-full object-contain"
          />
        </div>
      );
    }

    if (previewKind === "video") {
      return (
        <div className="overflow-hidden rounded-md border border-gray-200 bg-black">
          <video src={previewUrl} controls className="max-h-[36rem] w-full" />
        </div>
      );
    }

    if (previewKind === "audio") {
      return (
        <div className="rounded-md border border-gray-200 bg-gray-50 p-6">
          <audio src={previewUrl} controls className="w-full" />
        </div>
      );
    }

    return (
      <div className="overflow-hidden rounded-md border border-gray-200 bg-white">
        <iframe
          src={previewUrl}
          title={`Preview of ${version.fileName}`}
          className="h-[36rem] w-full"
        />
      </div>
    );
  };

  // We should prefer processingResult data if it is available and successful. 
  // Otherwise, we fallback to processingStatus and then finally document's own data.
  const aiStatus = processingResult?.status ?? processingStatus?.status;
  
  // Use the API document summary if it exists, otherwise use AI summary
  const displaySummary = document.summary || processingResult?.summary || processingStatus?.extractedSummary;
  const displayCategory = document.category || processingResult?.category || processingStatus?.classifiedCategory;
  const displayTags = document.tags.length > 0 ? document.tags : (processingResult?.tags || processingStatus?.generatedTags || []);
  
  // Try to use metadata from the processingResult if available
  let aiMetadata = processingResult?.metadata;
  // If not in result, fallback to status
  if (!aiMetadata && processingStatus?.generatedMetadata) {
      aiMetadata = processingStatus.generatedMetadata;
  }
  const aiError = processingResult?.error ?? processingStatus?.error;

  return (
    <div className="flex flex-col gap-8">
      <div>
        <Button onClick={() => navigate(-1)} variant="ghost">
          Back
        </Button>
      </div>

      <Card>
        <CardContent className="pt-6">
          <div className="flex items-start justify-between mb-6">
            <div>
              {isEditingMetadata ? (
                <div className="space-y-2">
                  <label className="block text-sm font-medium text-gray-700">
                    Name
                  </label>
                  <input
                    type="text"
                    value={metadataForm.name}
                    onChange={(event) =>
                      handleMetadataChange("name", event.target.value)
                    }
                    className="w-full rounded-md border border-gray-300 px-3 py-2 text-2xl font-bold text-gray-900"
                  />
                </div>
              ) : (
                <h1 className="text-2xl font-bold text-gray-900">
                  {document.name}
                </h1>
              )}
              <div className="mt-1 flex items-center gap-2 text-sm text-gray-500">
                <span>{document.fileName}</span>
                <Badge variant="secondary">
                  v{document.currentVersionNumber}
                </Badge>
              </div>
            </div>
            <div className="flex space-x-2">
              {canEdit &&
                (isEditingMetadata ? (
                  <>
                    <Button
                      onClick={() => void handleMetadataSave()}
                      disabled={updateDocument.isPending}
                    >
                      {updateDocument.isPending ? "Saving..." : "Save Details"}
                    </Button>
                    <Button
                      onClick={handleMetadataCancel}
                      variant="outline"
                      disabled={updateDocument.isPending}
                    >
                      Cancel
                    </Button>
                  </>
                ) : (
                  <Button
                    onClick={() => {
                      setMetadataError("");
                      setMetadataForm(toMetadataForm(document));
                      setIsEditingMetadata(true);
                    }}
                    variant="outline"
                  >
                    Edit Details
                  </Button>
                ))}
              <Button
                onClick={() =>
                  handleDownload(document.fileId, document.fileName)
                }
              >
                Download
              </Button>
              {canEdit && (
                <Button
                  onClick={() => {
                    setVersionError("");
                    setShowVersionUploader((prev) => !prev);
                  }}
                  variant="outline"
                >
                  {showVersionUploader
                    ? "Cancel Version Upload"
                    : "Upload New Version"}
                </Button>
              )}
              {canShare && (
                <Button
                  onClick={() => setShowShareDialog(true)}
                  variant="outline"
                >
                  Share
                </Button>
              )}
              {canDelete && (
                <Button onClick={handleDelete} variant="destructive">
                  Delete
                </Button>
              )}
            </div>
          </div>

          {isEditingMetadata ? (
            <div className="mb-6 grid gap-4">
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700">
                  Description
                </label>
                <textarea
                  value={metadataForm.description}
                  onChange={(event) =>
                    handleMetadataChange("description", event.target.value)
                  }
                  rows={3}
                  className="w-full rounded-md border border-gray-300 px-3 py-2 text-gray-700"
                />
              </div>

              <div className="grid gap-4 md:grid-cols-2">
                <div>
                  <label className="mb-1 block text-sm font-medium text-gray-700">
                    Category
                  </label>
                  <input
                    type="text"
                    value={metadataForm.category}
                    onChange={(event) =>
                      handleMetadataChange("category", event.target.value)
                    }
                    className="w-full rounded-md border border-gray-300 px-3 py-2 text-gray-700"
                  />
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-gray-700">
                    Tags
                  </label>
                  <input
                    type="text"
                    value={metadataForm.tags}
                    onChange={(event) =>
                      handleMetadataChange("tags", event.target.value)
                    }
                    placeholder="Comma-separated tags"
                    className="w-full rounded-md border border-gray-300 px-3 py-2 text-gray-700"
                  />
                </div>
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700">
                  Summary
                </label>
                <textarea
                  value={metadataForm.summary}
                  onChange={(event) =>
                    handleMetadataChange("summary", event.target.value)
                  }
                  rows={4}
                  className="w-full rounded-md border border-gray-300 px-3 py-2 text-gray-700"
                />
              </div>

              {metadataError && (
                <p className="text-sm text-red-600">{metadataError}</p>
              )}
            </div>
          ) : (
            document.description && (
              <div className="mb-6">
                <h2 className="text-sm font-medium text-gray-700 mb-1">
                  Description
                </h2>
                <p className="text-gray-600">{document.description}</p>
              </div>
            )
          )}

          {!isEditingMetadata &&
            (displayCategory ||
              displayTags.length > 0 ||
              displaySummary) && (
              <div className="mb-6 grid gap-4 md:grid-cols-2">
                <div>
                  <h2 className="text-sm font-medium text-gray-700 mb-1">
                    Category
                  </h2>
                  <p className="text-gray-600">
                    {displayCategory || "Not set"}
                  </p>
                </div>
                <div>
                  <h2 className="text-sm font-medium text-gray-700 mb-2">
                    Tags
                  </h2>
                  {displayTags.length > 0 ? (
                    <div className="flex flex-wrap gap-2">
                      {displayTags.map((tag) => (
                        <Badge key={tag} variant="outline">
                          {tag}
                        </Badge>
                      ))}
                    </div>
                  ) : (
                    <p className="text-gray-600">No tags</p>
                  )}
                </div>
                <div className="md:col-span-2">
                  <h2 className="text-sm font-medium text-gray-700 mb-1">
                    Summary
                  </h2>
                  <p className="text-gray-600 whitespace-pre-wrap">
                    {displaySummary || "No summary provided"}
                  </p>
                </div>
              </div>
            )}

          <div className="mb-6 rounded-md border border-gray-200 bg-gray-50 p-4">
            <div className="flex items-center justify-between gap-3">
              <h2 className="text-sm font-medium text-gray-700">
                AI Processing Info
              </h2>
              <Badge variant="outline">
                {aiStatus ? aiStatus.toLowerCase() : "pending"}
              </Badge>
            </div>

            {!processingResult && !processingStatus ? (
              <p className="mt-2 text-sm text-gray-500">
                AI processing has not started yet or the result is still being
                fetched.
              </p>
            ) : (
              <div className="mt-3 grid gap-4">
                <div className="md:col-span-2">
                  <h3 className="text-xs font-semibold uppercase tracking-wide text-gray-500">
                    Extracted Metadata JSON
                  </h3>
                  {aiMetadata ? (
                    <pre className="mt-2 overflow-x-auto whitespace-pre-wrap break-words rounded-md bg-white p-3 text-xs text-gray-700 border border-gray-200">
                      {aiMetadata}
                    </pre>
                  ) : (
                    <p className="mt-1 text-sm text-gray-700">No AI metadata available yet.</p>
                  )}
                </div>

                {aiError && (
                  <div className="md:col-span-2">
                    <h3 className="text-xs font-semibold uppercase tracking-wide text-red-500">
                      Error
                    </h3>
                    <p className="mt-1 text-sm text-red-600">{aiError}</p>
                  </div>
                )}
              </div>
            )}
          </div>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
            <div>
              <h3 className="text-sm font-medium text-gray-500">Size</h3>
              <p className="text-gray-900">
                {formatFileSize(document.fileSize)}
              </p>
            </div>
            <div>
              <h3 className="text-sm font-medium text-gray-500">Type</h3>
              <p className="text-gray-900">{document.mimeType}</p>
            </div>
            <div>
              <h3 className="text-sm font-medium text-gray-500">Owner</h3>
              <p className="text-gray-900">{document.ownerName}</p>
            </div>
            <div>
              <h3 className="text-sm font-medium text-gray-500">Created</h3>
              <p className="text-gray-900">{formatDate(document.createdAt)}</p>
            </div>
          </div>

          <div className="mb-6">
            <div className="flex items-center justify-between mb-3">
              <div>
                <h2 className="text-sm font-medium text-gray-700">Preview</h2>
                {selectedPreviewVersion && (
                  <p className="mt-1 text-xs text-gray-500">
                    Showing version {selectedPreviewVersion.versionNumber}:{" "}
                    {selectedPreviewVersion.fileName}
                  </p>
                )}
              </div>
              {selectedPreviewVersion && (
                <Button
                  onClick={() =>
                    handleDownload(
                      selectedPreviewVersion.fileId,
                      selectedPreviewVersion.fileName,
                    )
                  }
                  variant="outline"
                >
                  Download This Version
                </Button>
              )}
            </div>

            {selectedPreviewVersion ? (
              renderPreview(selectedPreviewVersion)
            ) : (
              <div className="rounded-md border border-gray-200 bg-gray-50 p-6 text-sm text-gray-600">
                No file available to preview.
              </div>
            )}
          </div>

          <div className="mb-6">
            <div className="flex items-center justify-between mb-3">
              <h2 className="text-sm font-medium text-gray-700">
                Version History
              </h2>
              <span className="text-xs text-gray-500">
                {document.versions.length} versions
              </span>
            </div>

            {showVersionUploader && (
              <div
                ref={versionUploaderRef}
                className="mb-4 rounded-md border border-gray-200 bg-gray-50 p-4"
              >
                <p className="mb-3 text-sm text-gray-600">
                  Uploading a new file will create an immutable version and make
                  it the current one.
                </p>
                <FileUploader onUploadComplete={handleVersionUpload} />
                {addDocumentVersion.isPending && (
                  <p className="mt-3 text-sm text-gray-600">
                    Saving new version...
                  </p>
                )}
                {versionError && (
                  <p className="mt-3 text-sm text-red-600">{versionError}</p>
                )}
              </div>
            )}

            <div className="space-y-3">
              {document.versions.map((version) => (
                <div
                  key={version.id}
                  className="flex flex-col gap-3 rounded-md border border-gray-200 p-4 md:flex-row md:items-center md:justify-between"
                >
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-semibold text-gray-900">
                        Version {version.versionNumber}
                      </span>
                      {version.versionNumber ===
                        document.currentVersionNumber && (
                        <Badge className="bg-green-100 text-green-700 hover:bg-green-100">
                          Current
                        </Badge>
                      )}
                    </div>
                    <p className="mt-1 text-sm text-gray-600">
                      {version.fileName}
                    </p>
                    <div className="mt-1 flex flex-wrap gap-3 text-xs text-gray-500">
                      <span>{formatFileSize(version.fileSize)}</span>
                      <span>{version.mimeType}</span>
                      <span>Uploaded by {version.createdByName}</span>
                      <span>{formatDate(version.createdAt)}</span>
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <Button
                      onClick={() => setSelectedPreviewVersionId(version.id)}
                      variant={
                        version.id === selectedPreviewVersionId
                          ? "default"
                          : "outline"
                      }
                    >
                      Preview
                    </Button>
                    <Button
                      onClick={() =>
                        handleDownload(version.fileId, version.fileName)
                      }
                      variant="outline"
                    >
                      Download Version
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="mb-6">
            <h2 className="text-sm font-medium text-gray-700 mb-2">
              Your Permissions
            </h2>
            <div className="flex flex-wrap gap-2">
              {document.permissions.map((perm) => (
                <Badge
                  key={perm}
                  className="bg-green-100 text-green-800 hover:bg-green-100"
                >
                  {perm}
                </Badge>
              ))}
            </div>
          </div>

          {(document.shares.length > 0 || canShare) && (
            <div>
              <h2 className="text-sm font-medium text-gray-700 mb-2">
                Shared With
              </h2>

              {document.shares.length === 0 ? (
                <p className="text-sm text-gray-500">
                  This document is private.
                </p>
              ) : (
                <div className="space-y-2">
                  {document.shares.map((share) => (
                    <div
                      key={share.id}
                      className="flex items-start justify-between gap-4 rounded-md bg-gray-50 p-3"
                    >
                      <div>
                        <span className="text-sm font-medium text-gray-900">
                          {share.shareType}:{" "}
                          {share.targetName || share.targetId || "Everyone"}
                        </span>
                        <div className="mt-1 flex flex-wrap gap-1">
                          {share.permissions.map((perm) => (
                            <Badge key={perm} variant="secondary">
                              {perm}
                            </Badge>
                          ))}
                        </div>
                      </div>

                      {canShare && (
                        <Button
                          type="button"
                          onClick={() => void handleRemoveShare(share.id)}
                          disabled={isSharingBusy}
                          variant="destructive"
                        >
                          Remove access
                        </Button>
                      )}
                    </div>
                  ))}
                </div>
              )}

              {shareError && (
                <p className="mt-3 text-sm text-red-600">
                  {shareError instanceof Error
                    ? shareError.message
                    : "Unable to update sharing right now."}
                </p>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      <ShareDialog
        isOpen={showShareDialog}
        onClose={() => setShowShareDialog(false)}
        onShare={handleShare}
      />
    </div>
  );
}

export default DocumentDetail;
