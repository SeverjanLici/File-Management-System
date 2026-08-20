import { useCallback, useRef, useState } from "react";
import * as tus from "tus-js-client";
import { endpoints } from "../config/api";
import { useAuth } from "react-oidc-context";

type UploadStatus =
  | "idle"
  | "uploading"
  | "paused"
  | "success"
  | "error"
  | "canceled";

interface StatusEntry {
  id: string;
  progress: number;
  bytesUploaded: number;
  bytesTotal: number;
  status: UploadStatus;
  uploadUrl?: string;
  error?: string;
  fileInfo?: {
    fileId: string;
    fileName: string;
    fileSize: number;
    mimeType: string;
  };
}

export function useTusUploader() {
  const auth = useAuth();
  const uploadsRef = useRef<Record<string, any>>({});
  const resolversRef = useRef<
    Record<string, { resolve: (v: any) => void; reject: (e: any) => void }>
  >({});
  const [statusMap, setStatusMap] = useState<Record<string, StatusEntry>>({});

  const setEntry = useCallback((id: string, patch: Partial<StatusEntry>) => {
    setStatusMap((prev) => ({
      ...(prev || {}),
      [id]: { ...(prev?.[id] || ({} as StatusEntry)), ...patch },
    }));
  }, []);

  const upload = useCallback(
    (file: File, metadata?: Record<string, string>) => {
      const id = `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
      let capturedFileId = "";

      const uploadObj = new tus.Upload(file, {
        endpoint: endpoints.files.upload,
        chunkSize: 5 * 1024 * 1024,
        retryDelays: [0, 1000, 3000, 5000],
        metadata: {
          filename: file.name,
          filetype: file.type,
          ...(metadata || {}),
        },
        headers: {
          Authorization: `Bearer ${auth.user?.access_token}`,
        },
        onAfterResponse: function (_req, res) {
          const fileIdHeader =
            res.getHeader("x-file-id") ?? res.getHeader("X-File-Id");
          if (fileIdHeader) {
            capturedFileId = fileIdHeader;
          }
        },
        onError: function (err) {
          setEntry(id, { status: "error", error: String(err) });
          const r = resolversRef.current[id];
          if (r) r.reject(err);
        },
        onProgress: function (bytesUploaded: number, bytesTotal: number) {
          const progress = bytesTotal
            ? Math.floor((bytesUploaded / bytesTotal) * 100)
            : 0;
          setEntry(id, {
            progress,
            bytesUploaded,
            bytesTotal,
            status: "uploading",
          });
        },
        onSuccess: function () {
          const normalizedFileId = (capturedFileId || "").trim();

          if (!normalizedFileId) {
            const err = new Error(
              "Upload completed but file identifier was not returned. Please retry upload.",
            );
            setEntry(id, { status: "error", error: err.message });
            const r = resolversRef.current[id];
            if (r) r.reject(err);
            return;
          }

          const info = {
            fileId: normalizedFileId,
            fileName: file.name,
            fileSize: file.size,
            mimeType: file.type || "application/octet-stream",
          };

          setEntry(id, {
            status: "success",
            progress: 100,
            uploadUrl: uploadObj.url || undefined,
            fileInfo: info,
          });
          const r = resolversRef.current[id];
          if (r) r.resolve(info);
        },
      });

      // store references
      uploadsRef.current[id] = uploadObj;
      setEntry(id, {
        id,
        progress: 0,
        bytesUploaded: 0,
        bytesTotal: file.size,
        status: "idle",
      });

      const promise = new Promise<any>((resolve, reject) => {
        resolversRef.current[id] = { resolve, reject };
      });

      // start upload async
      try {
        uploadObj.start();
        setEntry(id, { status: "uploading" });
      } catch (err) {
        setEntry(id, { status: "error", error: String(err) });
        const r = resolversRef.current[id];
        if (r) r.reject(err);
      }

      return { id, promise };
    },
    [auth.user?.access_token, setEntry],
  );

  const pause = useCallback(
    (id: string) => {
      const u = uploadsRef.current[id];
      if (u && typeof u.abort === "function") {
        try {
          u.abort();
          setEntry(id, { status: "paused" });
        } catch (e) {
          setEntry(id, { status: "error", error: String(e) });
        }
      }
    },
    [setEntry],
  );

  const resume = useCallback(
    (id: string) => {
      const u = uploadsRef.current[id];
      if (u && typeof u.start === "function") {
        try {
          u.start();
          setEntry(id, { status: "uploading" });
        } catch (e) {
          setEntry(id, { status: "error", error: String(e) });
        }
      }
    },
    [setEntry],
  );

  const cancel = useCallback(
    (id: string) => {
      const u = uploadsRef.current[id];
      if (u && typeof u.abort === "function") {
        try {
          u.abort(true);
          setEntry(id, { status: "canceled" });
          const r = resolversRef.current[id];
          if (r) r.reject(new Error("canceled"));
        } catch (e) {
          setEntry(id, { status: "error", error: String(e) });
        }
      }
    },
    [setEntry],
  );

  return {
    upload,
    pause,
    resume,
    cancel,
    statusMap,
  };
}

export default useTusUploader;
