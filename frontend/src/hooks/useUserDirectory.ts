import { useQuery } from "@tanstack/react-query";
import { useApi } from "./useApi";
import { endpoints } from "../config/api";
import type { PagedResponse, UserSummary } from "../types";

export function useUsers(
  search = "",
  page = 0,
  size = 20,
  enabled = search.trim().length > 0,
) {
  const { get } = useApi();

  return useQuery({
    queryKey: ["directory-users", search, page, size],
    queryFn: async () => {
      const params = new URLSearchParams({
        page: String(page),
        size: String(size),
      });

      if (search.trim()) {
        params.set("search", search.trim());
      }

      const response = await get<PagedResponse<UserSummary>>(
        `${endpoints.users.list}?${params.toString()}`,
      );
      if (!response.success) throw new Error(response.error?.message);
      return response.data!;
    },
    enabled,
  });
}
