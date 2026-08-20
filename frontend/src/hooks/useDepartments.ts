import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useApi } from "./useApi";
import { endpoints } from "../config/api";
import type {
  CreateDepartmentRequest,
  Department,
  DepartmentMemberRequest,
  DocumentSummary,
  PagedResponse,
  UserSummary,
} from "../types";

export function useDepartments(page = 0, size = 20) {
  const { get } = useApi();

  return useQuery({
    queryKey: ["departments", page, size],
    queryFn: async () => {
      const response = await get<PagedResponse<Department>>(
        `${endpoints.departments.list}?page=${page}&size=${size}`,
      );
      if (!response.success) throw new Error(response.error?.message);
      return response.data!;
    },
  });
}

export function useDepartment(id: string) {
  const { get } = useApi();

  return useQuery({
    queryKey: ["department", id],
    queryFn: async () => {
      const response = await get<Department>(endpoints.departments.get(id));
      if (!response.success) throw new Error(response.error?.message);
      return response.data!;
    },
    enabled: !!id,
  });
}

export function useDepartmentMembers(id: string, page = 0, size = 20) {
  const { get } = useApi();

  return useQuery({
    queryKey: ["department-members", id, page, size],
    queryFn: async () => {
      const response = await get<PagedResponse<UserSummary>>(
        `${endpoints.departments.members(id)}?page=${page}&size=${size}`,
      );
      if (!response.success) throw new Error(response.error?.message);
      return response.data!;
    },
    enabled: !!id,
  });
}

export function useDepartmentDocuments(id: string, page = 0, size = 20) {
  const { get } = useApi();

  return useQuery({
    queryKey: ["department-documents", id, page, size],
    queryFn: async () => {
      const response = await get<PagedResponse<DocumentSummary>>(
        `${endpoints.documents.byDepartment(id)}?page=${page}&size=${size}`,
      );
      if (!response.success) throw new Error(response.error?.message);
      return response.data!;
    },
    enabled: !!id,
  });
}

export function useCreateDepartment() {
  const { post } = useApi();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: CreateDepartmentRequest) => {
      const response = await post<Department>(
        endpoints.departments.create,
        data,
      );
      if (!response.success) throw new Error(response.error?.message);
      return response.data!;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["departments"] });
    },
  });
}

export function useDeleteDepartment() {
  const { del } = useApi();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: string) => {
      const response = await del(endpoints.departments.delete(id));
      if (!response.success) throw new Error(response.error?.message);
    },
    onSuccess: (_, departmentId) => {
      queryClient.invalidateQueries({ queryKey: ["departments"] });
      queryClient.invalidateQueries({ queryKey: ["documents"] });
      queryClient.invalidateQueries({ queryKey: ["my-documents"] });
      queryClient.invalidateQueries({ queryKey: ["document"] });
      queryClient.removeQueries({ queryKey: ["department", departmentId] });
      queryClient.removeQueries({
        queryKey: ["department-members", departmentId],
      });
      queryClient.removeQueries({
        queryKey: ["department-documents", departmentId],
      });
    },
  });
}

export function useAddDepartmentMember() {
  const { post } = useApi();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      departmentId,
      data,
    }: {
      departmentId: string;
      data: DepartmentMemberRequest;
    }) => {
      const response = await post<Department>(
        endpoints.departments.addMember(departmentId),
        data,
      );
      if (!response.success) throw new Error(response.error?.message);
      return response.data!;
    },
    onSuccess: (_, { departmentId }) => {
      queryClient.invalidateQueries({ queryKey: ["department", departmentId] });
      queryClient.invalidateQueries({
        queryKey: ["department-members", departmentId],
      });
      queryClient.invalidateQueries({ queryKey: ["departments"] });
    },
  });
}

export function useRemoveDepartmentMember() {
  const { del } = useApi();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      departmentId,
      userId,
    }: {
      departmentId: string;
      userId: string;
    }) => {
      const response = await del(
        endpoints.departments.removeMember(departmentId, userId),
      );
      if (!response.success) throw new Error(response.error?.message);
    },
    onSuccess: (_, { departmentId }) => {
      queryClient.invalidateQueries({ queryKey: ["department", departmentId] });
      queryClient.invalidateQueries({
        queryKey: ["department-members", departmentId],
      });
      queryClient.invalidateQueries({ queryKey: ["departments"] });
    },
  });
}
