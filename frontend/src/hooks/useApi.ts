import { useAuth } from "react-oidc-context";
import type { ApiResponse } from "@/types";

export function useApi() {
  const auth = useAuth();

  const trace = (title: string, details: string) => {
    console.log(`\n\n\n==================== FRONTEND API: ${title} ====================\n${details}\n================================================================\n\n\n`);
  };

  const getHeaders = (): HeadersInit => {
    const headers: HeadersInit = {
      "Content-Type": "application/json",
    };
    if (auth.user?.access_token) {
      headers["Authorization"] = `Bearer ${auth.user.access_token}`;
    }
    return headers;
  };

  const request = async <T>(
    url: string,
    options: RequestInit = {},
  ): Promise<ApiResponse<T>> => {
    trace("REQUEST START", `method=${options.method ?? "GET"}\nurl=${url}\nauthenticated=${!!auth.user?.access_token}`);
    const response = await fetch(url, {
      ...options,
      headers: {
        ...getHeaders(),
        ...options.headers,
      },
    });

    trace("RESPONSE RECEIVED", `method=${options.method ?? "GET"}\nurl=${url}\nstatus=${response.status}\nok=${response.ok}`);

    const contentType = response.headers.get("content-type") ?? "";
    const hasJsonBody = contentType.includes("application/json");
    const hasNoBody = response.status === 204 || response.status === 205;

    if (!response.ok) {
      const error = hasJsonBody
        ? await response.json().catch(() => ({
            success: false,
            error: { code: "NETWORK_ERROR", message: "Network error occurred" },
          }))
        : {
            success: false,
            error: {
              code: "NETWORK_ERROR",
              message: response.statusText || "Network error occurred",
            },
            timestamp: new Date().toISOString(),
          };
      trace("REQUEST FAILED", `method=${options.method ?? "GET"}\nurl=${url}\nstatus=${response.status}\nbody=${JSON.stringify(error, null, 2)}`);
      return error;
    }

    if (hasNoBody || !hasJsonBody) {
      trace("NO JSON BODY", `method=${options.method ?? "GET"}\nurl=${url}`);
      return {
        success: true,
        timestamp: new Date().toISOString(),
      };
    }

    const json = await response.json();
    trace("REQUEST OK", `method=${options.method ?? "GET"}\nurl=${url}\nbody=${JSON.stringify(json, null, 2)}`);
    return json;
  };

  const get = <T>(url: string) => request<T>(url, { method: "GET" });

  const post = <T>(url: string, data?: unknown) =>
    request<T>(url, {
      method: "POST",
      body: data ? JSON.stringify(data) : undefined,
    });

  const put = <T>(url: string, data?: unknown) =>
    request<T>(url, {
      method: "PUT",
      body: data ? JSON.stringify(data) : undefined,
    });

  const del = <T>(url: string) => request<T>(url, { method: "DELETE" });

  return {
    get,
    post,
    put,
    del,
    getHeaders,
    accessToken: auth.user?.access_token,
  };
}
