import { useState, useEffect } from "react";
import { endpoints } from "../config/api";
import { useApi } from "./useApi";

export interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
  timestamp: Date;
}

export function useAiChat(chatKey: string = "global") {
  // Load initial messages from localStorage if available
  const [messages, setMessages] = useState<ChatMessage[]>(() => {
    const saved = localStorage.getItem(`ai-chat-${chatKey}`);
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        // Convert string timestamps back to Date objects
        return parsed.map((m: any) => ({
          ...m,
          timestamp: new Date(m.timestamp),
        }));
      } catch (e) {
        return [];
      }
    }
    return [];
  });
  
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { post } = useApi();

  // Save messages to localStorage whenever they change
  useEffect(() => {
    localStorage.setItem(`ai-chat-${chatKey}`, JSON.stringify(messages));
  }, [messages, chatKey]);

  const askAboutFile = async (fileId: string, question: string): Promise<string> => {
    setLoading(true);
    setError(null);

    // Optimistically add the user message
    const userMessage: ChatMessage = {
      id: Date.now().toString() + "_user",
      role: "user",
      content: question,
      timestamp: new Date(),
    };
    
    setMessages((prev) => [...prev, userMessage]);

    try {
      const response = await post<{ answer: string; sources?: string[] }>(
        endpoints.ai.askFile(fileId),
        { question }
      );

      if (!response.success) {
        throw new Error(response.error?.message || "Failed to get answer");
      }

      const assistantMessage: ChatMessage = {
        id: Date.now().toString() + "_assistant",
        role: "assistant",
        content: response.data?.answer || "No answer received",
        timestamp: new Date(),
      };

      setMessages((prev) => [...prev, assistantMessage]);
      return response.data?.answer || "";
    } catch (err: any) {
      const errorMsg = err?.message || "Failed to get answer";
      setError(errorMsg);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const askGlobal = async (question: string): Promise<string> => {
    setLoading(true);
    setError(null);

    // Optimistically add the user message
    const userMessage: ChatMessage = {
      id: Date.now().toString() + "_user",
      role: "user",
      content: question,
      timestamp: new Date(),
    };

    setMessages((prev) => [...prev, userMessage]);

    try {
      const response = await post<{ answer: string; sources?: string[] }>(
        endpoints.ai.askGlobal,
        { question }
      );

      if (!response.success) {
        throw new Error(response.error?.message || "Failed to get answer");
      }

      const assistantMessage: ChatMessage = {
        id: Date.now().toString() + "_assistant",
        role: "assistant",
        content: response.data?.answer || "No answer received",
        timestamp: new Date(),
      };

      setMessages((prev) => [...prev, assistantMessage]);
      return response.data?.answer || "";
    } catch (err: any) {
      const errorMsg = err?.message || "Failed to get answer";
      setError(errorMsg);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const clearMessages = () => {
    setMessages([]);
    setError(null);
    localStorage.removeItem(`ai-chat-${chatKey}`);
  };

  return {
    messages,
    loading,
    error,
    askAboutFile,
    askGlobal,
    clearMessages,
  };
}
