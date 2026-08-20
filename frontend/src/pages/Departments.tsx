import { useState } from "react";
import { Link } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useApi } from "../hooks/useApi";
import { useCurrentUser } from "../hooks/useUser";
import { endpoints } from "../config/api";
import type {
  Department,
  PagedResponse,
  CreateDepartmentRequest,
} from "../types";
import Loading from "../components/Loading";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Field, FieldLabel, FieldContent } from "@/components/ui/field";

function Departments() {
  const { get, post } = useApi();
  const { data: user } = useCurrentUser();
  const queryClient = useQueryClient();
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [newDepartment, setNewDepartment] = useState<CreateDepartmentRequest>({
    name: "",
    description: "",
  });

  const { data, isLoading } = useQuery({
    queryKey: ["departments"],
    queryFn: async () => {
      const response = await get<PagedResponse<Department>>(
        endpoints.departments.list,
      );
      if (!response.success) throw new Error(response.error?.message);
      return response.data!;
    },
  });

  const createDepartment = useMutation({
    mutationFn: async (data: CreateDepartmentRequest) => {
      const response = await post<Department>(
        endpoints.departments.create,
        data,
      );
      if (!response.success) throw new Error(response.error?.message);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["departments"] });
      setShowCreateForm(false);
      setNewDepartment({ name: "", description: "" });
    },
  });

  if (isLoading) return <Loading />;

  const isAdmin = user?.role === "ADMIN";

  const closeCreateDialog = () => {
    setShowCreateForm(false);
    setNewDepartment({ name: "", description: "" });
  };

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    createDepartment.mutate(newDepartment);
  };

  return (
    <div className="flex flex-col gap-8">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Departments</h1>
        {isAdmin && (
          <Button onClick={() => setShowCreateForm(true)}>
            Create Department
          </Button>
        )}
      </div>

      <Dialog
        open={showCreateForm}
        onOpenChange={(open) => !open && closeCreateDialog()}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Create Department</DialogTitle>
            <DialogDescription>
              Add a new department and make it available for document sharing
              and workspace management.
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handleCreate} className="space-y-4">
            <Field>
              <FieldLabel>
                <Label>Name</Label>
              </FieldLabel>
              <FieldContent>
                <Input
                  type="text"
                  value={newDepartment.name}
                  onChange={(e) =>
                    setNewDepartment((prev) => ({
                      ...prev,
                      name: e.target.value,
                    }))
                  }
                  required
                />
              </FieldContent>
            </Field>

            <Field>
              <FieldLabel>
                <Label>Description</Label>
              </FieldLabel>
              <FieldContent>
                <textarea
                  value={newDepartment.description}
                  onChange={(e) =>
                    setNewDepartment((prev) => ({
                      ...prev,
                      description: e.target.value,
                    }))
                  }
                  className="w-full rounded-lg border border-input bg-transparent px-2.5 py-2 text-base transition-colors outline-none placeholder:text-muted-foreground"
                  rows={3}
                />
              </FieldContent>
            </Field>

            <DialogFooter>
              <Button
                variant="outline"
                type="button"
                onClick={closeCreateDialog}
                disabled={createDepartment.isPending}
              >
                Cancel
              </Button>
              <Button type="submit" disabled={createDepartment.isPending}>
                {createDepartment.isPending
                  ? "Creating..."
                  : "Create Department"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {data?.content.length === 0 ? (
        <Card>
          <CardContent className="text-center py-12">
            <p className="text-gray-500">No departments yet</p>
            {isAdmin && (
              <div className="mt-4">
                <Button onClick={() => setShowCreateForm(true)}>
                  Create the first department
                </Button>
              </div>
            )}
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {data?.content.map((dept) => (
            <Link
              key={dept.id}
              to={`/departments/${dept.id}`}
              className="block"
            >
              <Card className="transition-shadow hover:shadow-md">
                <CardContent>
                  <div>
                    <h3 className="text-lg font-semibold text-gray-900">
                      {dept.name}
                    </h3>
                    {dept.description && (
                      <p className="mt-1 text-sm text-gray-600">
                        {dept.description}
                      </p>
                    )}
                  </div>
                  <div className="mt-4 flex items-center text-sm text-gray-500">
                    <span>{dept.memberCount} members</span>
                  </div>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}

export default Departments;
