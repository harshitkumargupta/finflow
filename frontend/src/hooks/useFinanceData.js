import { useEffect, useState } from "react";
import { getMyAccounts } from "../api/accountApi";
import { getAutopayAuthorizations } from "../api/autopayApi";
import { getBills, getBudgets, getGoals } from "../api/financeApi";
import useApiResource from "./useApiResource";

const list = { initialData: [] };

export const useAccounts = (options) => useApiResource(getMyAccounts, { ...list, ...options });
export const useBudgets = (options) => useApiResource(getBudgets, { ...list, ...options });
export const useBills = (options) => useApiResource(getBills, { ...list, ...options });
export const useGoals = (options) => useApiResource(getGoals, { ...list, ...options });
export const useAutopays = (options) => useApiResource(getAutopayAuthorizations, { ...list, ...options });

export function combineResources(...resources) {
  return {
    loading: resources.some((resource) => resource.loading),
    error: resources.map((resource) => resource.error).find(Boolean) || "",
    reload: () => Promise.all(resources.map((resource) => resource.reload())),
  };
}

export function useSelectedAccountId(accounts, requestedId) {
  const [accountId, setAccountId] = useState(requestedId || "");
  useEffect(() => {
    if (!accounts.length) return;
    setAccountId((current) => (accounts.some((account) => String(account.id) === current) ? current : String(accounts[0].id)));
  }, [accounts]);
  return [accountId, setAccountId];
}
