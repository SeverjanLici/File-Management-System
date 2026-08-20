import { Link } from "react-router-dom";
import type { DocumentSummary } from "../types";
import { useProcessingResult, useProcessingStatus } from "@/hooks/useProcessing";
import {
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  CardFooter,
} from "@/components/ui/card";
import { Badge } from "./ui/badge";

interface DocumentCardProps {
  document: DocumentSummary;
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return "0 Bytes";
  const k = 1024;
  const sizes = ["Bytes", "KB", "MB", "GB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
}

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString("en-US", {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

function getFileIcon(mimeType: string): string {
  if (mimeType.startsWith("image/")) return "🖼️";
  if (mimeType.startsWith("video/")) return "🎬";
  if (mimeType.startsWith("audio/")) return "🎵";
  if (mimeType.includes("pdf")) return "📄";
  if (mimeType.includes("word") || mimeType.includes("document")) return "📝";
  if (mimeType.includes("sheet") || mimeType.includes("excel")) return "📊";
  if (mimeType.includes("presentation") || mimeType.includes("powerpoint"))
    return "📽️";
  if (mimeType.includes("zip") || mimeType.includes("archive")) return "📦";
  return "📁";
}

function DocumentCard({ document }: DocumentCardProps) {
  const fileId = document.fileId ?? document.id;
  
  const { data: processingStatusData, error: processingError } = useProcessingStatus(fileId);
  const { data: processingResult } = useProcessingResult(fileId);
  
  const aiStatus = processingResult?.status ?? processingStatusData?.status;
  const displaySummary = document.summary || processingResult?.summary || processingStatusData?.extractedSummary;
  const errorMsg = processingResult?.error || processingStatusData?.error;

  const processingBadge = (() => {
    if (!aiStatus && processingError) return null;
    if (!aiStatus) return <Badge variant="outline">AI pending</Badge>;

    if (aiStatus === "COMPLETED") {
      return <Badge className="ml-2 bg-green-600">AI ready</Badge>;
    }
    if (aiStatus === "FAILED") {
      return <Badge variant="destructive">AI failed</Badge>;
    }
    return <Badge variant="outline">AI {aiStatus.toLowerCase()}</Badge>;
  })();


  return (
    <>
      <Link to={`/documents/${document.id}`} className="block h-full">
        <Card className="bg-white hover:shadow transition-shadow h-full flex flex-col">
          <CardHeader className="flex flex-row items-center gap-2 px-4 py-2 flex-none">
            <div className="text-2xl">{getFileIcon(document.mimeType)}</div>
            <div className="flex-1 min-w-0">
              <CardTitle className="text-sm font-medium text-gray-900 truncate">
                {document.name ?? document.fileName ?? "Untitled document"}
                <Badge variant="outline" className="ml-2">
                  v{document.currentVersionNumber ?? 1}
                </Badge>
                {processingBadge}
              </CardTitle>
            </div>
          </CardHeader>

          <CardContent className="px-4 flex-1">
            {displaySummary ? (
              <p className="mt-1 text-xs text-gray-500 whitespace-normal break-words line-clamp-3">
                {displaySummary}
              </p>
            ) : aiStatus === "PENDING" || aiStatus === "PROCESSING" ? (
              <p className="mt-1 text-xs text-gray-400 italic">
                AI summary is being generated...
              </p>
            ) : aiStatus === "FAILED" ? (
              <p className="mt-1 text-xs text-gray-400 italic">
                Failed to generate AI summary.
              </p>
            ) : (
              <p className="mt-1 text-xs text-gray-400 italic">
                No summary available.
              </p>
            )}
            
            {aiStatus === "FAILED" && errorMsg && (
              <p className="mt-1 text-xs text-red-600 truncate">
                {errorMsg}
              </p>
            )}
          </CardContent>

          <CardFooter className="flex-none mt-auto">
            <div className="flex flex-col w-full gap-3">
              <div className="flex items-center justify-between text-xs text-gray-500">
                <span>{formatFileSize(document.fileSize)}</span>
                <span>{formatDate(document.createdAt)}</span>
              </div>
              <div className="text-xs text-gray-400">
                by {document.ownerName}
              </div>
            </div>
          </CardFooter>
         </Card>
       </Link>
    </>
  );
}

export default DocumentCard;
