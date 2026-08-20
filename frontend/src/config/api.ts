export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api/v1";

export const endpoints = {
  // User endpoints
  users: {
    me: `${API_BASE_URL}/users/me`,
    list: `${API_BASE_URL}/users`,
    get: (id: string) => `${API_BASE_URL}/users/${id}`,
    roles: `${API_BASE_URL}/users/roles`,
  },
  // Department endpoints
  departments: {
    list: `${API_BASE_URL}/departments`,
    get: (id: string) => `${API_BASE_URL}/departments/${id}`,
    create: `${API_BASE_URL}/departments`,
    update: (id: string) => `${API_BASE_URL}/departments/${id}`,
    delete: (id: string) => `${API_BASE_URL}/departments/${id}`,
    members: (id: string) => `${API_BASE_URL}/departments/${id}/members`,
    addMember: (id: string) => `${API_BASE_URL}/departments/${id}/members`,
    removeMember: (id: string, userId: string) =>
      `${API_BASE_URL}/departments/${id}/members/${userId}`,
  },
  // Document endpoints
  documents: {
    list: `${API_BASE_URL}/documents`,
    my: `${API_BASE_URL}/documents/my`,
    byDepartment: (departmentId: string) =>
      `${API_BASE_URL}/documents/departments/${departmentId}`,
    get: (id: string) => `${API_BASE_URL}/documents/${id}`,
    create: `${API_BASE_URL}/documents`,
    update: (id: string) => `${API_BASE_URL}/documents/${id}`,
    addVersion: (id: string) => `${API_BASE_URL}/documents/${id}/versions`,
    delete: (id: string) => `${API_BASE_URL}/documents/${id}`,
    share: (id: string) => `${API_BASE_URL}/documents/${id}/share`,
    shares: (id: string) => `${API_BASE_URL}/documents/${id}/shares`,
    removeShare: (id: string, shareId: string) =>
      `${API_BASE_URL}/documents/${id}/shares/${shareId}`,
    companyWide: (id: string) => `${API_BASE_URL}/documents/${id}/company-wide`,
  },
  // File endpoints
  files: {
    upload: `${API_BASE_URL}/upload`,
    complete: (id: string) => `${API_BASE_URL}/upload/${id}/complete`,
    get: (id: string) => `${API_BASE_URL}/files/${id}`,
    download: (id: string) => `${API_BASE_URL}/files/${id}/download`,
    presigned: (id: string) => `${API_BASE_URL}/files/${id}/presigned`,
    preview: (id: string) => `${API_BASE_URL}/files/${id}/preview`,
    my: `${API_BASE_URL}/files/my`,
  },
  // AI processing endpoints
  processing: {
    statusByFile: (fileId: string) =>
      `${API_BASE_URL}/processing/status/file/${fileId}`,

    resultByFile: (fileId: string) =>
      `${API_BASE_URL}/processing/result/file/${fileId}`,

    metrics: `${API_BASE_URL}/processing/metrics`,

    list: `${API_BASE_URL}/processing`, // ⚠️ serve endpoint backend

    retry: (id: string) =>
      `${API_BASE_URL}/processing/retry/${id}`,

    validate: (id: string) =>
      `${API_BASE_URL}/processing/validate/${id}`,

    fail: (id: string) =>
      `${API_BASE_URL}/processing/fail/${id}`,
    edit: (id: string) =>
      `${API_BASE_URL}/processing/edit/${id}`,
  },
  // AI Chat endpoints
  ai: {
    askFile: (fileId: string) =>
      `${API_BASE_URL}/ai/files/${fileId}/ask`,
    askGlobal: `${API_BASE_URL}/ai/ask-global`,
  },
};