import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useCurrentUser } from "../hooks/useUser";
import {
  useAddDepartmentMember,
  useDepartment,
  useDeleteDepartment,
  useDepartmentDocuments,
  useDepartmentMembers,
  useRemoveDepartmentMember,
} from "../hooks/useDepartments";
import { useUsers } from "../hooks/useUserDirectory";
import DocumentCard from "../components/DocumentCard";
import Loading from "../components/Loading";
import { Button } from "@/components/ui/button";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Field, FieldLabel, FieldContent } from "@/components/ui/field";
import { Badge } from "@/components/ui/badge";

function DepartmentDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: currentUser } = useCurrentUser();
  const {
    data: department,
    isLoading: isDepartmentLoading,
    error: departmentError,
  } = useDepartment(id!);
  const { data: members, isLoading: isMembersLoading } = useDepartmentMembers(
    id!,
  );
  const { data: documents, isLoading: isDocumentsLoading } =
    useDepartmentDocuments(id!);
  const [selectedUserId, setSelectedUserId] = useState("");
  const { data: userResults } = useUsers("", 0, 100, true);
  const addMember = useAddDepartmentMember();
  const removeMember = useRemoveDepartmentMember();
  const deleteDepartment = useDeleteDepartment();

  if (isDepartmentLoading || isMembersLoading || isDocumentsLoading) {
    return <Loading message="Loading department workspace..." />;
  }

  if (departmentError || !department) {
    return (
      <div className="rounded-lg bg-white p-8 shadow">
        <p className="text-red-600">Department not found.</p>
        <button
          onClick={() => navigate("/departments")}
          className="mt-4 text-blue-600 hover:text-blue-700"
        >
          Back to departments
        </button>
      </div>
    );
  }

  const canManageMembers =
    currentUser?.role === "ADMIN" || currentUser?.role === "MANAGER";
  const isAdmin = currentUser?.role === "ADMIN";
  const memberIds = new Set(
    (members?.content ?? []).map((member) => member.id),
  );
  const addableUsers = (userResults?.content ?? []).filter(
    (user) => !memberIds.has(user.id),
  );

  const handleAddMember = () => {
    if (!selectedUserId) return;

    addMember.mutate(
      {
        departmentId: department.id,
        data: { userId: selectedUserId },
      },
      {
        onSuccess: () => {
          setSelectedUserId("");
        },
      },
    );
  };

  const handleDeleteDepartment = async () => {
    if (
      !window.confirm(`Are you sure you want to delete "${department.name}"?`)
    ) {
      return;
    }

    await deleteDepartment.mutateAsync(department.id);
    navigate("/departments");
  };

  return (
    <div className="flex flex-col gap-8">
      <div className="flex items-center justify-between">
        <div>
          <Link to="/departments">
            <Button variant="ghost">Back to departments</Button>
          </Link>
          <h1 className="mt-2 text-2xl font-bold text-gray-900">
            {department.name}
          </h1>
          <p className="mt-1 text-sm text-gray-600">
            {department.description || "No department description provided."}
          </p>
        </div>
        <div className="flex items-start gap-3">
          {isAdmin && (
            <Button
              variant="destructive"
              onClick={() => void handleDeleteDepartment()}
              disabled={deleteDepartment.isPending}
            >
              {deleteDepartment.isPending ? "Deleting..." : "Delete Department"}
            </Button>
          )}
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
        <Card>
          <CardHeader>
            <CardTitle>Department Documents</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-gray-500 mb-4">
              Shared with this department
            </p>

            {documents && documents.content.length > 0 ? (
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                {documents.content.map((document) => (
                  <DocumentCard key={document.id} document={document} />
                ))}
              </div>
            ) : (
              <div className="rounded-md border border-dashed border-gray-300 bg-gray-50 p-6 text-sm text-gray-600">
                No documents are currently shared with this department.
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Members</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-gray-500 mb-4">
              {members?.totalElements ?? 0} people
            </p>

            {canManageMembers && (
              <div className="mb-6">
                <Field>
                  <FieldLabel>
                    <Label>Add member</Label>
                  </FieldLabel>
                  <FieldContent>
                    <div className="flex gap-3">
                      <select
                        value={selectedUserId}
                        onChange={(event) =>
                          setSelectedUserId(event.target.value)
                        }
                        className="h-9 w-full rounded-lg border border-input bg-transparent px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
                      >
                        <option value="">Select a user</option>
                        {addableUsers.map((user) => (
                          <option key={user.id} value={user.id}>
                            {`${user.firstName} ${user.lastName}`.trim() ||
                              user.email}{" "}
                            ({user.email})
                          </option>
                        ))}
                      </select>

                      <Button
                        type="button"
                        onClick={handleAddMember}
                        disabled={!selectedUserId || addMember.isPending}
                      >
                        {addMember.isPending ? "Adding..." : "Add user"}
                      </Button>
                    </div>
                  </FieldContent>
                </Field>
              </div>
            )}

            <div className="space-y-3">
              {(members?.content ?? []).map((member) => (
                <div
                  key={member.id}
                  className="flex items-center justify-between rounded-md border border-gray-200 px-3 py-3"
                >
                  <div>
                    <p className="text-sm font-medium text-gray-900">
                      {member.firstName} {member.lastName}
                    </p>
                    <p className="text-xs text-gray-500">{member.email}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge variant="outline">{member.role}</Badge>
                    {canManageMembers && (
                      <Button
                        size="sm"
                        variant="ghost"
                        className="text-red-600"
                        onClick={() =>
                          removeMember.mutate({
                            departmentId: department.id,
                            userId: member.id,
                          })
                        }
                      >
                        Remove
                      </Button>
                    )}
                  </div>
                </div>
              ))}

              {(members?.content ?? []).length === 0 && (
                <div className="rounded-md border border-dashed border-gray-300 bg-gray-50 p-6 text-sm text-gray-600">
                  No members in this department yet.
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

export default DepartmentDetail;
