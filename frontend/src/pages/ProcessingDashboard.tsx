import { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import {
    getProcessingMetrics,
    getProcessingList,
    retryProcessing,
    validateProcessing,
    editProcessing,
} from "../config/processingApi";

type Processing = {
    id: string;
    fileId: string;
    status: string;
    extractedSummary?: string;
    classifiedCategory?: string;
    generatedTags?: string[];
    error?: string;
    executionTimeMs?: number;
    retryCount?:number;
    validated: boolean;
    validatedBy?: string;
};

export default function ProcessingDashboard() {
    const auth = useAuth();
    const token = auth.user?.access_token;

    const [metrics, setMetrics] = useState<any>({});
    const [list, setList] = useState<Processing[]>([]);
    const [selected, setSelected] = useState<Processing | null>(null);

    const [editing, setEditing] = useState<Processing | null>(null);
    const [editForm, setEditForm] = useState({
        extractedSummary: "",
        classifiedCategory: "",
        generatedTags: ""
    });

    useEffect(() => {
        if (token) {
            fetchMetrics();
            fetchList();
        }
    }, [token]);

    const fetchMetrics = async () => {
        if (!token) return;
        const data = await getProcessingMetrics(token);
        setMetrics(data);
    };

    const fetchList = async () => {
        if (!token) return;
        const data = await getProcessingList(token);
        setList(data);
    };

    const retry = async (id: string) => {
        if (!token) return;
        await retryProcessing(id, token);
        fetchList();
    };

    const validate = async (id: string) => {
        if (!token) return;
        await validateProcessing(id, token);
        fetchList();
    };



    return (
        <div className="p-6">
        <h1 className="text-2xl font-bold mb-6">Processing Dashboard</h1>
        {/* 📊 METRICS */}
        <div className="grid grid-cols-5 gap-4 mb-6">
        <div className="bg-white p-4 shadow rounded">
            Total: {metrics.total}
        </div>
        <div className="bg-white p-4 shadow rounded">
            Completed: {metrics.completed}
        </div>
        <div className="bg-white p-4 shadow rounded">
            Failed: {metrics.failed}
        </div>
        <div className="bg-white p-4 shadow rounded">
            Success: {((metrics.successRate ?? 0) * 100).toFixed(1)}%
        </div>
        <div className="bg-white p-4 shadow rounded">
            Avg time: {metrics.avgExecutionTimeMs ?? 0} ms
        </div>
        </div>

        {/* 📄 LISTA */}
        <table className="w-full bg-white shadow rounded">
        <thead>
            <tr className="text-left border-b">
        <th className="p-2">FileId</th>
            <th className="p-2">Status</th>
            <th className="p-2">Time</th>
            <th className="p-2">Retry</th>
            <th className="p-2">Actions</th>
            <th className="p-2">Validation</th>
            </tr>
            </thead>

            <tbody>
            {list.map((p) => (
                    <tr key={p.id} className="border-b">
                <td className="p-2">{p.fileId}</td>
                    <td className="p-2">{p.status}</td>
                    <td className="p-2">{p.executionTimeMs ?? "-"}</td>
                    <td className="p-2">{p.retryCount ?? "-"}</td>

                    <td className="p-2 flex gap-2">
                        <button
                            onClick={() => setSelected(p)}
                            className="px-3 py-1.5 bg-rasparent border border-[#6d7ea1] text-black rounded-tl-xl rounded-br-xl hover:bg-[#6d7eff] transition-colors"
                        >
                            View
                        </button>

                        <button
                            onClick={() => retry(p.id)}
                            className="px-3 py-1.5 bg-trasparent border border-[#cccaa1] text-black rounded-tl-xl rounded-br-xl hover:bg-[#e8de1e] transition-colors"
                        >
                            Retry
                        </button>

                        <button
                            onClick={() => validate(p.id)}
                            disabled={p.validated || p.status !== "COMPLETED"}
                            className={`px-3 py-1.5 border rounded-tl-xl rounded-br-xl transition-colors
                                ${p.validated
                                ? "border-gray-300 text-gray-400 cursor-not-allowed"
                                : "border-[#a9de9c] hover:bg-[#5ee83b]"}
                                `}
                        >
                            Validate
                        </button>
                        <button
                            onClick={() => {
                                setEditing(p);
                                setEditForm({
                                    extractedSummary: p.extractedSummary || "",
                                    classifiedCategory: p.classifiedCategory || "",
                                    generatedTags: p.generatedTags?.join(", ") || ""
                                });
                            }}
                            className="px-3 py-1.5 border border-blue-300 rounded-tl-xl rounded-br-xl hover:bg-blue-200"
                        >
                            Edit
                        </button>

            </td>
            <td className="p-2">
                    {p.validated ? (
                    <span className="text-green-600 font-semibold">✔ Validated</span>
                    ) : (
                    <span className="text-yellow-600 font-semibold">⏳ Pending</span>
                    )}
            </td>
            </tr>
    ))}
        </tbody>
        </table>


        {selected && (
            <div className="mt-6 p-4 bg-gray-100 rounded">
            <h2 className="font-bold mb-2">Details</h2>

                <p>
                <b>Summary:</b> {selected.extractedSummary}
        </p>

        <p>
        <b>Category:</b> {selected.classifiedCategory}
        </p>

        <p>
        <b>Tags:</b> {selected.generatedTags?.join(", ")}
        </p>
        <p>
            <b>Validation:</b>{" "}
            {selected.validated ? (
                <span className="text-green-600">
            Validated by {selected.validatedBy ?? "unknown"}
        </span>
                ) : (
                    <span className="text-yellow-600">Not validated</span>
                )}
        </p>
            {selected.error && (
                <p className="text-red-600">
                    <b>Error:</b> {selected.error}
            </p>
            )}
            </div>
        )}

        {/* EDIT FORM */}
            {editing && (
                <div className="mt-6 p-4 bg-white shadow rounded">
                    <h2 className="font-bold mb-4">Edit Metadata</h2>

                    <div className="flex flex-col gap-3">

                        <input
                            type="text"
                            placeholder="Summary"
                            value={editForm.extractedSummary}
                            onChange={(e) =>
                                setEditForm({ ...editForm, extractedSummary: e.target.value })
                            }
                            className="border p-2 rounded"
                        />

                        <input
                            type="text"
                            placeholder="Category"
                            value={editForm.classifiedCategory}
                            onChange={(e) =>
                                setEditForm({ ...editForm, classifiedCategory: e.target.value })
                            }
                            className="border p-2 rounded"
                        />

                        <input
                            type="text"
                            placeholder="Tags (comma separated)"
                            value={editForm.generatedTags}
                            onChange={(e) =>
                                setEditForm({ ...editForm, generatedTags: e.target.value })
                            }
                            className="border p-2 rounded"
                        />

                        <div className="flex gap-2 mt-2">
                            <button
                                onClick={async () => {
                                    if (!token || !editing) return;

                                    await editProcessing(editing.id, token, {
                                        extractedSummary: editForm.extractedSummary,
                                        classifiedCategory: editForm.classifiedCategory,
                                        generatedTags: editForm.generatedTags
                                            .split(",")
                                            .map((t) => t.trim()),
                                    });

                                    setEditing(null);
                                    fetchList();
                                }}
                                className={`px-3 py-1.5 border rounded-tl-xl rounded-br-xl transition-colors "border-[#a9de9c] hover:bg-[#5ee83b]`}
                            >
                                Save
                            </button>

                            <button
                                onClick={() => setEditing(null)}
                                className={`px-3 py-1.5 border rounded-tl-xl rounded-br-xl transition-colors border-gray hover:bg-gray-400`}
                            >
                                Cancel
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>


    );

}