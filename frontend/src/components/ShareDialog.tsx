import { useEffect, useMemo, useState } from "react";
import { useDepartments } from "../hooks/useDepartments";
import { useCurrentUser } from "../hooks/useUser";
import { useUsers } from "../hooks/useUserDirectory";
import type { ShareType, Permission, CreateShareRequest } from "../types";
import { Button } from "./ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "./ui/dialog";
import { Label } from "./ui/label";

interface ShareDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onShare: (data: CreateShareRequest) => Promise<void>;
}

function ShareDialog({ isOpen, onClose, onShare }: ShareDialogProps) {
  const [shareType, setShareType] = useState<ShareType>("USER");
  const [permissions, setPermissions] = useState<Permission[]>(["VIEW"]);
  const [selectedUserId, setSelectedUserId] = useState("");
  const [selectedDepartmentId, setSelectedDepartmentId] = useState("");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { data: departmentsPage } = useDepartments(0, 100);
  const { data: usersPage } = useUsers("", 0, 100, isOpen);
  const { data: currentUser } = useCurrentUser();

  const departments = departmentsPage?.content ?? [];
  const users = useMemo(
    () =>
      (usersPage?.content ?? []).filter((user) => user.id !== currentUser?.id),
    [currentUser?.id, usersPage?.content],
  );

  useEffect(() => {
    if (!isOpen) {
      setShareType("USER");
      setPermissions(["VIEW"]);
      setSelectedUserId("");
      setSelectedDepartmentId("");
      setError("");
      setIsSubmitting(false);
    }
  }, [isOpen]);

  useEffect(() => {
    if (selectedUserId && !users.some((user) => user.id === selectedUserId)) {
      setSelectedUserId("");
    }
  }, [selectedUserId, users]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const request = buildRequest();
    if (!request) {
      return;
    }

    setError("");
    setIsSubmitting(true);

    try {
      await onShare(request);
      onClose();
    } catch (submitError) {
      setError(
        submitError instanceof Error
          ? submitError.message
          : "Failed to update sharing.",
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const togglePermission = (permission: Permission) => {
    setPermissions((prev) =>
      prev.includes(permission)
        ? prev.filter((p) => p !== permission)
        : [...prev, permission],
    );
  };

  const buildRequest = (): CreateShareRequest | null => {
    if (permissions.length === 0) {
      setError("Select at least one permission.");
      return null;
    }

    if (shareType === "USER") {
      const user = users.find((entry) => entry.id === selectedUserId);
      if (!user) {
        setError("Select a user to share with.");
        return null;
      }

      return {
        shareType,
        targetId: user.id,
        targetName: `${user.firstName} ${user.lastName}`.trim() || user.email,
        permissions,
      };
    }

    if (shareType === "DEPARTMENT") {
      const department = departments.find(
        (entry) => entry.id === selectedDepartmentId,
      );
      if (!department) {
        setError("Select a department to share with.");
        return null;
      }

      return {
        shareType,
        targetId: department.id,
        targetName: department.name,
        permissions,
      };
    }

    return {
      shareType,
      targetName: "Everyone",
      permissions,
    };
  };

  const renderTargetField = () => {
    if (shareType === "USER") {
      return (
        <div className="mb-4">
          <Label className="mb-1">User</Label>
          <select
            value={selectedUserId}
            onChange={(e) => setSelectedUserId(e.target.value)}
            className="h-9 w-full rounded-lg border border-input bg-transparent px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
            required
          >
            <option value="">Select a user</option>
            {users.map((user) => (
              <option key={user.id} value={user.id}>
                {`${user.firstName} ${user.lastName}`.trim() || user.email} (
                {user.email})
              </option>
            ))}
          </select>
        </div>
      );
    }

    if (shareType === "DEPARTMENT") {
      return (
        <div className="mb-4">
          <Label className="mb-1">Department</Label>
          <select
            value={selectedDepartmentId}
            onChange={(e) => setSelectedDepartmentId(e.target.value)}
            className="h-9 w-full rounded-lg border border-input bg-transparent px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
            required
          >
            <option value="">Select a department</option>
            {departments.map((department) => (
              <option key={department.id} value={department.id}>
                {department.name}
              </option>
            ))}
          </select>
        </div>
      );
    }

    return (
      <div className="mb-4 rounded-md border border-blue-100 bg-blue-50 p-3 text-sm text-blue-700">
        This grants access to everyone in the company.
      </div>
    );
  };

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Share Document</DialogTitle>
          <DialogDescription>
            Grant access to a user, department, or the entire company.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <Label className="mb-1">Share with</Label>
            <select
              value={shareType}
              onChange={(e) => {
                setShareType(e.target.value as ShareType);
                setError("");
              }}
              className="h-9 w-full rounded-lg border border-input bg-transparent px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
            >
              <option value="USER">User</option>
              <option value="DEPARTMENT">Department</option>
              <option value="COMPANY">Company (Everyone)</option>
            </select>
          </div>

          {renderTargetField()}

          <div>
            <Label className="mb-2">Permissions</Label>
            <div className="grid gap-2 rounded-lg border border-border p-3">
              {(["VIEW", "EDIT", "DELETE", "SHARE"] as Permission[]).map(
                (perm) => (
                  <label
                    key={perm}
                    className="flex items-center gap-2 text-sm text-foreground"
                  >
                    <input
                      type="checkbox"
                      checked={permissions.includes(perm)}
                      onChange={() => togglePermission(perm)}
                      className="size-4 rounded border-input text-primary accent-primary focus-visible:ring-2 focus-visible:ring-ring"
                    />
                    <span>{perm}</span>
                  </label>
                ),
              )}
            </div>
          </div>

          {error && <p className="text-sm text-red-600">{error}</p>}

          <DialogFooter>
            <Button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              variant="outline"
            >
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? "Sharing..." : "Share"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

export default ShareDialog;
