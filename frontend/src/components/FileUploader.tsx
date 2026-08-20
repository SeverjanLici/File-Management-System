import { useState } from "react";
import useTusUploader from "../hooks/useTusUploader";

interface FileUploaderProps {
  onUploadComplete: (fileInfo: {
    fileId: string;
    fileName: string;
    fileSize: number;
    mimeType: string;
  }) => void;
}

function FileUploader({ onUploadComplete }: FileUploaderProps) {
  const { upload, pause, resume, cancel, statusMap } = useTusUploader();
  const [currentId, setCurrentId] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] || null;
    setSelectedFile(file);
    if (!file) return;

    const { id, promise } = upload(file);
    setCurrentId(id);
    try {
      const info = await promise;
      onUploadComplete(info);
    } catch (err) {
      // swallow — statusMap contains error
    }
  };

  const entry = currentId ? statusMap[currentId] : undefined;

  return (
    <div className="p-4 border rounded">
      <label className="block text-sm font-medium text-gray-700 mb-2">
        Select file to upload
      </label>
      <input type="file" onChange={handleFileChange} />

      {selectedFile && (
        <div className="mt-4">
          <div className="text-sm">
            {selectedFile.name} — {(selectedFile.size / 1024).toFixed(1)} KB
          </div>

          <div className="mt-2">
            <div className="w-full bg-gray-200 rounded h-3">
              <div
                className="bg-blue-600 h-3 rounded"
                style={{ width: `${entry?.progress ?? 0}%` }}
              />
            </div>
            <div className="text-xs text-gray-600 mt-1">
              {entry?.progress ?? 0}%
            </div>
          </div>

          <div className="mt-3 flex space-x-2">
            {entry?.status === "uploading" && (
              <button
                onClick={() => currentId && pause(currentId)}
                className="px-3 py-1 border rounded"
              >
                Pause
              </button>
            )}
            {entry?.status === "paused" && (
              <button
                onClick={() => currentId && resume(currentId)}
                className="px-3 py-1 border rounded"
              >
                Resume
              </button>
            )}
            {(entry?.status === "uploading" || entry?.status === "paused") && (
              <button
                onClick={() => currentId && cancel(currentId)}
                className="px-3 py-1 border rounded"
              >
                Cancel
              </button>
            )}
            {entry?.status === "success" && (
              <div className="text-sm text-green-700">Upload complete</div>
            )}
            {entry?.status === "error" && (
              <div className="text-sm text-red-700">{entry.error}</div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

export default FileUploader;
