import { endpoints } from "./api";

export const getProcessingMetrics = async (token: string) => {
    const res = await fetch(endpoints.processing.metrics, {
        headers: { Authorization: `Bearer ${token}` },
    });

    if (!res.ok) throw new Error("Failed to fetch metrics");

    return res.json();
};

export const getProcessingList = async (token: string) => {
    const res = await fetch(endpoints.processing.list, {
        headers: { Authorization: `Bearer ${token}` },
    });

    if (!res.ok) throw new Error("Failed to fetch list");

    return res.json();
};

export const retryProcessing = async (id: string, token: string) => {
    const res = await fetch(endpoints.processing.retry(id), {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
    });

    if (!res.ok) throw new Error("Retry failed");
};

export const validateProcessing = async (id: string, token: string) => {
    const res = await fetch(endpoints.processing.validate(id), {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
    });

    if (!res.ok) throw new Error("Validation failed");
};



export const editProcessing = async (
    id: string,
    token: string,
    data: {
        extractedSummary: string;
        classifiedCategory: string;
        generatedTags: string[];
    }
) => {
    await fetch(endpoints.processing.edit(id), {
        method: "POST",
        headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
        },
        body: JSON.stringify(data),
    });
};
