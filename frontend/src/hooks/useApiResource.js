import { useCallback, useEffect, useRef, useState } from "react";
import { getApiErrorMessage } from "../api/axios";

export default function useApiResource(fetcher, { initialData = null, immediate = true } = {}) {
  const [data, setData] = useState(initialData);
  const [loading, setLoading] = useState(immediate);
  const [error, setError] = useState("");
  const fetcherRef = useRef(fetcher);
  const mounted = useRef(true);
  fetcherRef.current = fetcher;

  useEffect(() => {
    mounted.current = true;
    return () => { mounted.current = false; };
  }, []);

  const reload = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const response = await fetcherRef.current();
      if (mounted.current) setData(response.data);
      return response.data;
    } catch (cause) {
      if (mounted.current) setError(getApiErrorMessage(cause));
      throw cause;
    } finally {
      if (mounted.current) setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (immediate) reload().catch(() => {});
  }, [immediate, reload]);

  return { data, setData, loading, error, reload };
}
