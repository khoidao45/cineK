import { useCallback } from "react";
import { AxiosError } from "axios";
import type { ErrorResponse } from "../types/api";

export function useErrorMessage() {
  const parseError = useCallback((error: unknown): string => {
    const axiosErr = error as AxiosError<ErrorResponse>;
    return (
      axiosErr?.response?.data?.message ||
      axiosErr?.message ||
      "Something went wrong."
    );
  }, []);

  return { parseError };
}
