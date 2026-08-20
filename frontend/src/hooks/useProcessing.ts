import { useQuery } from "@tanstack/react-query";
import { endpoints } from "../config/api";
import { useApi } from "./useApi";
import type { ApiResponse, ProcessingResult, ProcessingStatusResponse } from "../types";

async function fetchWithAuth<T>(url: string, token?: string): Promise<T> {
  const response = await fetch(url, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });

  if (response.status === 404) {
    throw new Error("PROCESSING_NOT_FOUND");
  }

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `Failed request: ${response.status}`);
  }

  return response.json();
}

export function useProcessingResult(fileId?: string) {
  const { accessToken } = useApi();

  return useQuery({
    queryKey: ["processing-result", fileId],
    queryFn: async (): Promise<ProcessingResult | null> => {
      try {
        const response = await fetchWithAuth<ApiResponse<ProcessingResult>>(
          endpoints.processing.resultByFile(fileId!),
          accessToken,
        );
        return response.data || null;
      } catch (error) {
        if (error instanceof Error && error.message === "PROCESSING_NOT_FOUND") {
          return null;
        }
        throw error;
      }
    },
    enabled: !!fileId && !!accessToken,
    retry: false,
    staleTime: 30_000,
  });
}

export function useProcessingStatus(fileId?: string) {
  const { accessToken } = useApi();

  return useQuery({
    queryKey: ["processing-status", fileId],
    queryFn: async (): Promise<ProcessingStatusResponse | null> => {
      try {
        const response = await fetchWithAuth<ApiResponse<ProcessingStatusResponse>>(
          endpoints.processing.statusByFile(fileId!),
          accessToken,
        );
        return response.data || null;
      } catch (error) {
        if (error instanceof Error && error.message === "PROCESSING_NOT_FOUND") {
          return null;
        }
        throw error;
      }
    },
    enabled: !!fileId && !!accessToken,
    retry: false,
    staleTime: 10_000,
    refetchInterval: (query) => {
      const status = (query.state.data as ProcessingStatusResponse | null | undefined)?.status;
      return status === "PENDING" || status === "PROCESSING" ? 3000 : false;
    },
  });
}
