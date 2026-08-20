import { useState } from "react";
import { useNavigate } from "react-router-dom";
import FileUploader from "../components/FileUploader";
import { useCreateDocument } from "../hooks/useDocuments";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Field,
  FieldLabel,
  FieldContent,
  FieldDescription,
} from "@/components/ui/field";

interface UploadedFile {
  fileId: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
}

function Upload() {
  const navigate = useNavigate();
  const createDocument = useCreateDocument();
  const [uploadedFile, setUploadedFile] = useState<UploadedFile | null>(null);
  const [documentName, setDocumentName] = useState("");
  const [description, setDescription] = useState("");
  const [category, setCategory] = useState("");
  const [tagsInput, setTagsInput] = useState("");
  const [summary, setSummary] = useState("");
  const [error, setError] = useState("");
  const [uploaderKey, setUploaderKey] = useState(0);

  const parsedTags = tagsInput
    .split(",")
    .map((tag) => tag.trim())
    .filter(Boolean);

  const handleUploadComplete = (fileInfo: UploadedFile) => {
    setUploadedFile(fileInfo);
    setDocumentName(fileInfo.fileName.replace(/\.[^/.]+$/, ""));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    if (!uploadedFile) {
      setError("Please upload a file first");
      return;
    }

    if (!uploadedFile.fileId?.trim()) {
      setError("Upload is not complete yet. Please re-upload the file.");
      return;
    }

    try {
      const document = await createDocument.mutateAsync({
        name: documentName || uploadedFile.fileName,
        description: description || undefined,
        category: category.trim() || undefined,
        tags: parsedTags,
        summary: summary.trim() || undefined,
        fileId: uploadedFile.fileId,
        fileName: uploadedFile.fileName,
        fileSize: uploadedFile.fileSize,
        mimeType: uploadedFile.mimeType,
      });

      navigate(`/documents/${document.id}`);
    } catch (err: any) {
      setError(err?.message || "Failed to create document");
    }
  };

  const resetForm = () => {
    setUploadedFile(null);
    setDocumentName("");
    setDescription("");
    setCategory("");
    setTagsInput("");
    setSummary("");
    setUploaderKey((k) => k + 1);
  };

  return (
    <div className="flex flex-col gap-8">
      <h1 className="text-2xl font-bold text-gray-900">Upload Document</h1>

      <Card>
        <CardContent>
          <div className="grid grid-cols-2 gap-6">
            <div>
              <h2 className="text-lg font-medium mb-3">File</h2>
              <FileUploader
                key={uploaderKey}
                onUploadComplete={handleUploadComplete}
              />
            </div>

            <div>
              <h2 className="text-lg font-medium mb-3">Metadata</h2>

              {uploadedFile && (
                <div className="mb-4 p-4 bg-green-50 border border-green-200 rounded-md">
                  <p className="text-green-800">
                    File uploaded successfully:{" "}
                    <strong>{uploadedFile.fileName}</strong>
                  </p>
                </div>
              )}

              <form onSubmit={handleSubmit} className="flex flex-col gap-4">
                <Field>
                  <FieldLabel>
                    <Label>Document Name</Label>
                  </FieldLabel>
                  <FieldContent>
                    <Input
                      value={documentName}
                      onChange={(e) => setDocumentName(e.target.value)}
                      required
                    />
                  </FieldContent>
                </Field>

                <Field>
                  <FieldLabel>
                    <Label>Description (optional)</Label>
                  </FieldLabel>
                  <FieldContent>
                    <textarea
                      value={description}
                      onChange={(e) => setDescription(e.target.value)}
                      rows={3}
                      className="w-full rounded-lg border border-input bg-transparent px-2.5 py-2 text-base transition-colors outline-none placeholder:text-muted-foreground"
                    />
                  </FieldContent>
                </Field>

                <Field>
                  <FieldLabel>
                    <Label>Category (optional)</Label>
                  </FieldLabel>
                  <FieldContent>
                    <Input
                      value={category}
                      onChange={(e) => setCategory(e.target.value)}
                      placeholder="Policy, Finance, HR..."
                    />
                  </FieldContent>
                </Field>

                <Field>
                  <FieldLabel>
                    <Label>Tags (optional)</Label>
                  </FieldLabel>
                  <FieldContent>
                    <Input
                      value={tagsInput}
                      onChange={(e) => setTagsInput(e.target.value)}
                      placeholder="contract, onboarding, confidential"
                    />
                    <FieldDescription>
                      Separate tags with commas.
                    </FieldDescription>
                  </FieldContent>
                </Field>

                <Field>
                  <FieldLabel>
                    <Label>Summary (optional)</Label>
                  </FieldLabel>
                  <FieldContent>
                    <textarea
                      value={summary}
                      onChange={(e) => setSummary(e.target.value)}
                      rows={4}
                      placeholder="Short summary for collaborators and search results"
                      className="w-full rounded-lg border border-input bg-transparent px-2.5 py-2 text-base transition-colors outline-none placeholder:text-muted-foreground"
                    />
                  </FieldContent>
                </Field>

                {error && (
                  <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 rounded-md">
                    {error}
                  </div>
                )}

                <div className="flex items-center justify-between">
                  <Button variant="outline" type="button" onClick={resetForm}>
                    Upload Different File
                  </Button>
                  <Button
                    type="submit"
                    disabled={
                      createDocument.isPending ||
                      !uploadedFile ||
                      !uploadedFile.fileId?.trim()
                    }
                  >
                    {createDocument.isPending
                      ? "Creating..."
                      : "Create Document"}
                  </Button>
                </div>
              </form>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

export default Upload;
