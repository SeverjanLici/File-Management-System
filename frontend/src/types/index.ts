export type Role = "ADMIN" | "MANAGER" | "USER";

export type Permission = "VIEW" | "EDIT" | "DELETE" | "SHARE" | "ADMIN";

export type ShareType = "USER" | "DEPARTMENT" | "ROLE" | "COMPANY";

export type User = {
  id: string;
  externalId: string;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
  departments: Department[];
  createdAt: string;
  updatedAt: string;
};

export type UserSummary = {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
};

export type Department = {
  id: string;
  name: string;
  description?: string;
  memberCount: number;
  createdAt: string;
  updatedAt: string;
};

export type Document = {
  id: string;
  name: string;
  description?: string;
  category?: string;
  tags: string[];
  summary?: string;
  currentVersionNumber: number;
  fileId: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
  ownerId: string;
  ownerName: string;
  permissions: Permission[];
  shares: Share[];
  versions: DocumentVersion[];
  createdAt: string;
  updatedAt: string;
};

export type DocumentSummary = {
  id: string;
  name: string;
  category?: string;
  tags: string[];
  summary?: string;
  currentVersionNumber: number;
  fileId?: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
  ownerId: string;
  ownerName: string;
  createdAt: string;
};

export type Share = {
  id: string;
  documentId: string;
  shareType: ShareType;
  targetId?: string;
  targetName?: string;
  permissions: Permission[];
  createdAt: string;
  createdBy: string;
};

export type DocumentVersion = {
  id: string;
  versionNumber: number;
  fileId: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
  createdBy: string;
  createdByName: string;
  createdAt: string;
};

export type UploadedFile = {
  id: string;
  originalName: string;
  storagePath: string;
  size: number;
  mimeType: string;
  uploadedBy: string;
  uploadCompleted: boolean;
  createdAt: string;
};

export type ProcessingStatus = "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";

export type ProcessingStatusResponse = {
  id: string;
  fileId: string;
  status: ProcessingStatus;
  generatedMetadata?: string | null;
  extractedSummary?: string | null;
  classifiedCategory?: string | null;
  generatedTags: string[];
  error?: string | null;
  retryCount: number;
  createdAt: string;
  updatedAt: string;
  startedAt?: string | null;
  completedAt?: string | null;
};

export type ProcessingResult = {
  fileId: string;
  status: ProcessingStatus;
  summary?: string | null;
  category?: string | null;
  tags: string[];
  metadata?: string | null;
  error?: string | null;
};

export type PresignedUrl = {
  url: string;
  expiresAt: string;
};

export type ApiResponse<T> = {
  success: boolean;
  data?: T;
  error?: {
    code: string;
    message: string;
    details?: Record<string, unknown>;
  };
  timestamp: string;
};

export type PagedResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
};

export type CreateDocumentRequest = {
  name: string;
  description?: string;
  category?: string;
  tags: string[];
  summary?: string;
  fileId: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
};

export type CreateDocumentVersionRequest = {
  fileId: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
};

export type UpdateDocumentRequest = {
  name?: string;
  description?: string;
  category?: string;
  tags?: string[];
  summary?: string;
};

export interface CreateShareRequest {
  shareType: ShareType;
  targetId?: string;
  targetName?: string;
  permissions: Permission[];
}

export interface CreateDepartmentRequest {
  name: string;
  description?: string;
}

export interface DepartmentMemberRequest {
  userId: string;
}
