import { Navigate, Route, Routes } from "react-router-dom";
import { lazy, Suspense } from "react";
import DashboardLayout from "./layouts/DashboardLayout";
import ProtectedRoute from "./components/ProtectedRoute";
import Loading from "./components/Loading";

const Analytics = lazy(() => import("./pages/Analytics"));
const Accounts = lazy(() => import("./pages/Accounts"));
const Autopay = lazy(() => import("./pages/Autopay"));
const Bills = lazy(() => import("./pages/Bills"));
const Budgets = lazy(() => import("./pages/Budgets"));
const Cards = lazy(() => import("./pages/Cards"));
const Dashboard = lazy(() => import("./pages/Dashboard"));
const Deposit = lazy(() => import("./pages/Deposit"));
const Login = lazy(() => import("./pages/Login"));
const Goals = lazy(() => import("./pages/Goals"));
const GoalAssistant = lazy(() => import("./pages/GoalAssistant"));
const Family = lazy(() => import("./pages/Family"));
const Investments = lazy(() => import("./pages/Investments"));
const NotFound = lazy(() => import("./pages/NotFound"));
const Profile = lazy(() => import("./pages/Profile"));
const Notifications = lazy(() => import("./pages/Notifications"));
const Receipts = lazy(() => import("./pages/Receipts"));
const Reports = lazy(() => import("./pages/Reports"));
const Register = lazy(() => import("./pages/Register"));
const Transactions = lazy(() => import("./pages/Transactions"));
const Transfer = lazy(() => import("./pages/Transfer"));
const Withdraw = lazy(() => import("./pages/Withdraw"));
const Wallet = lazy(() => import("./pages/Wallet"));
const VerifyEmail = lazy(() => import("./pages/VerifyEmail"));

export default function App() {
  return (
    <Suspense fallback={<Loading />}>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/verify-email" element={<VerifyEmail />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<DashboardLayout />}>
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/accounts" element={<Accounts />} />
            <Route path="/budgets" element={<Budgets />} />
            <Route path="/bills" element={<Bills />} />
            <Route path="/goals" element={<Goals />} />
            <Route path="/goals/assistant" element={<GoalAssistant />} />
            <Route path="/reports" element={<Reports />} />
            <Route path="/wallet" element={<Wallet />} />
            <Route path="/family" element={<Family />} />
            <Route path="/cards" element={<Cards />} />
            <Route path="/autopay" element={<Autopay />} />
            <Route path="/investments" element={<Investments />} />
            <Route path="/market-news" element={<Investments />} />
            <Route path="/investment-education" element={<Investments />} />
            <Route path="/deposit" element={<Deposit />} />
            <Route path="/withdraw" element={<Withdraw />} />
            <Route path="/transfer" element={<Transfer />} />
            <Route path="/transactions" element={<Transactions />} />
            <Route path="/receipts" element={<Receipts />} />
            <Route path="/analytics" element={<Analytics />} />
            <Route path="/profile" element={<Profile />} />
            <Route path="/notifications" element={<Notifications />} />
          </Route>
        </Route>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="*" element={<NotFound />} />
      </Routes>
    </Suspense>
  );
}
