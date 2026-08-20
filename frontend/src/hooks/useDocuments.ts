import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useApi } from "./useApi";
import { endpoints } from "../config/api";
import type {
  Document,
  DocumentSummary,
  PagedResponse,
  CreateDocumentRequest,
  CreateDocumentVersionRequest,
  CreateShareRequest,
  UpdateDocumentRequest,
} from "../types";

type DocumentFilters = {
  search?: string;
  category?: string;
  tag?: string;
};

export function useDocuments(
  page = 0,
  size = 20,
  filters: DocumentFilters = {},
) {
  const { get } = useApi();
  const normalizedSearch = filters.search?.trim() ?? "";
  const normalizedCategory = filters.category?.trim() ?? "";
  const normalizedTag = filters.tag?.trim() ?? "";

  return useQuery({
    queryKey: [
      "documents",
      page,
      size,
      normalizedSearch,
      normalizedCategory,
      normalizedTag,
    ],
    queryFn: async () => {
      const params = new URLSearchParams({
        page: String(page),
        size: String(size),
      });

      if (normalizedSearch) params.set("search", normalizedSearch);
      if (normalizedCategory) params.set("category", normalizedCategory);
      if (normalizedTag) params.set("tag", normalizedTag);

      const response = await get<PagedResponse<DocumentSummary>>(
        `${endpoints.documents.list}?${params.toString()}`,
      );
      if (!response.success) throw new Error(response.error?.message);
      return response.data!;
    },
  });
}

export function useMyDocuments(page = 0, size = 20) {
  const { get } = useApi();

  return useQuery({
    queryKey: ["my-documents", page, size],
    queryFn: async () => {
      const response = await get<PagedResponse<DocumentSummary>>(
        `${endpoints.documents.my}?page=${page}&size=${size}`,
      );
      if (!response.success) throw new Error(response.error?.message);
      return response.data!;
    },
  });
}

export function useDocument(id: string) {
  const { get } = useApi();

  return useQuery({
    queryKey: ["document", id],
    queryFn: async () => {
      const response = await get<Document>(endpoints.documents.get(id));
      if (!response.success) throw new Error(response.error?.message);
      return response.data!;
    },
    enabled: !!id,
  });
}

export function useCreateDocument() {
  const { post } = useApi();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: CreateDocumentRequest) => {
      const response = await post<Document>(endpoints.documents.create, data);
      if (!response.success) throw new Error(response.error?.message);
      return response.data!;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["documents"] });
      queryClient.invalidateQueries({ queryKey: ["my-documents"] });
    },
  });
}

export function useUpdateDocument() {
  const { put } = useApi();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      documentId,
      data,
    }: {
      documentId: string;
      data: UpdateDocumentRequest;
    }) => {
      const response = await put<Document>(
        endpoints.documents.update(documentId),
        data,
      );
      if (!response.success) throw new Error(response.error?.message);
      return response.data!;
    },
    onSuccess: (_, { documentId }) => {
      queryClient.invalidateQueries({ queryKey: ["document", documentId] });
      queryClient.invalidateQueries({ queryKey: ["documents"] });
      queryClient.invalidateQueries({ queryKey: ["my-documents"] });
    },
  });
}

export function useShareDocument() {
  const { post } = useApi();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      documentId,
      data,
    }: {
      documentId: string;
      data: CreateShareRequest;
    }) => {
      const response = await post(endpoints.documents.share(documentId), data);
      if (!response.success) throw new Error(response.error?.message);
      return response.data;
    },
    onSuccess: (_, { documentId }) => {
      queryClient.invalidateQueries({ queryKey: ["document", documentId] });
    },
  });
}

export function useRemoveDocumentShare() {
  const { del } = useApi();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      documentId,
      shareId,
    }: {
      documentId: string;
      shareId: string;
    }) => {
      const response = await del(
        endpoints.documents.removeShare(documentId, shareId),
      );
      if (!response.success) throw new Error(response.error?.message);
    },
    onSuccess: (_, { documentId }) => {
      queryClient.invalidateQueries({ queryKey: ["document", documentId] });
    },
  });
}

export function useAddDocumentVersion() {
  const { post } = useApi();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      documentId,
      data,
    }: {
      documentId: string;
      data: CreateDocumentVersionRequest;
    }) => {
      const response = await post<Document>(
        endpoints.documents.addVersion(documentId),
        data,
      );
      if (!response.success) throw new Error(response.error?.message);
      return response.data!;
    },
    onSuccess: (_, { documentId }) => {
      queryClient.invalidateQueries({ queryKey: ["document", documentId] });
      queryClient.invalidateQueries({ queryKey: ["documents"] });
      queryClient.invalidateQueries({ queryKey: ["my-documents"] });
    },
  });
}

export function useDeleteDocument() {
  const { del } = useApi();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: string) => {
      const response = await del(endpoints.documents.delete(id));
      if (!response.success) throw new Error(response.error?.message);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["documents"] });
      queryClient.invalidateQueries({ queryKey: ["my-documents"] });
    },
  });
}
