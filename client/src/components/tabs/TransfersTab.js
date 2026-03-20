import { useEffect, useState } from "react";
import { api } from "../../api";

const TRANSFER_OPTIONS = [
  {
    id: "bof-customer-transfer",
    label: "BANK OF FIJI TRANSFER",
    description: "Transfer funds to another Bank of Fiji customer account within the same bank.",
  },
  {
    id: "local-bank-transfer",
    label: "LOCAL BANK TRANSFER",
    description: "Transfer funds to another local Fiji bank account.",
  },
  {
    id: "international-transfer",
    label: "INTERNATIONAL TRANSFER",
    description: "Set up overseas transfer details and foreign beneficiary information.",
  },
  {
    id: "wallet-provider-transfer",
    label: "TRANSFER TO WALLET PROVIDER",
    description: "Send funds to approved wallet providers in Fiji.",
  },

  {
    id: "transfer-limits",
    label: "TRANSFER LIMITS",
    description: "View your transfer limits for daily, weekly, and monthly periods.",
  },
];

const CUSTOMER_TRANSFER_LIMITS = {
  daily: 2000,
  weekly: 10000,
  monthly: 40000,
};

const FIJI_DIGITAL_WALLETS = [
  {
    name: "M-PAiSA",
    provider: "Vodafone Fiji",
  },
  {
    name: "MyCash",
    provider: "Digicel Fiji",
  },
  {
    name: "Inkk Mobile Wallet",
    provider: "HFC Bank",
  },
];

const FIJI_LOCAL_BANKS = [
  "ANZ Fiji",
  "Westpac Fiji",
  "BSP Financial Group",
  "HFC Bank",
  "BRED Bank Fiji",
  "Bank of Baroda Fiji",
  "Fiji Development Bank",
];

function toAmount(value) {
  const parsed = Number(value || 0);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 0;
}

export default function TransfersTab({
  accounts,
  transferForm,
  setTransferForm,
  onInitiateTransfer,
  pendingTransfer,
  setPendingTransfer,
  onVerifyTransfer,
  transferMessage,
}) {
  const [open, setOpen] = useState(true);
  const [activeOption, setActiveOption] = useState("bof-customer-transfer");
  const [destinationValidation, setDestinationValidation] = useState({
    status: "idle",
    customerName: "",
    accountNumber: "",
    message: "",
  });
  const [localBankForm, setLocalBankForm] = useState({ recipientName: "", accountNumber: "", bankName: "", amount: "", description: "" });
  const [localBankMessage, setLocalBankMessage] = useState("");

  const activeTransferOption = TRANSFER_OPTIONS.find((option) => option.id === activeOption) || TRANSFER_OPTIONS[0];

  const showTransferForm = activeOption === "bof-customer-transfer";
  const showLocalBankForm = activeOption === "local-bank-transfer";
  const showLimitsContent = activeOption === "transfer-limits";
  const showWalletContent = activeOption === "wallet-provider-transfer";
  const sourceAccount =
    accounts.find((account) => String(account.id) === String(transferForm.fromAccountId || "")) ||
    accounts[0] ||
    null;
  const normalizedToAccountNumber = String(transferForm.toAccountNumber || "").trim();
  const hasValidToAccountFormat = /^\d{12}$/.test(normalizedToAccountNumber) || /^\d+$/.test(normalizedToAccountNumber);
  const destinationIsValidated =
    destinationValidation.status === "success" &&
    destinationValidation.accountNumber === normalizedToAccountNumber;
  const currentTransferAmount = toAmount(transferForm.amount);
  const limitRows = [
    { label: "Daily", value: CUSTOMER_TRANSFER_LIMITS.daily },
    { label: "Weekly", value: CUSTOMER_TRANSFER_LIMITS.weekly },
    { label: "Monthly", value: CUSTOMER_TRANSFER_LIMITS.monthly },
  ];

  useEffect(() => {
    if (!showTransferForm) {
      return;
    }

    const fromAccountId = Number(transferForm.fromAccountId || 0);
    if (!fromAccountId || !normalizedToAccountNumber) {
      setDestinationValidation({ status: "idle", customerName: "", accountNumber: "", message: "" });
      return;
    }
    if (!hasValidToAccountFormat) {
      setDestinationValidation({
        status: "error",
        customerName: "",
        accountNumber: normalizedToAccountNumber,
        message: "Enter a 12-digit account number or numeric customer ID",
      });
      return;
    }

    let cancelled = false;
    const timeoutId = setTimeout(async () => {
      setDestinationValidation((prev) => ({
        ...prev,
        status: "loading",
        accountNumber: normalizedToAccountNumber,
        message: "Validating destination account...",
      }));

      try {
        const result = await api.validateTransferDestination({
          fromAccountId,
          toAccountNumber: normalizedToAccountNumber,
        });
        if (cancelled) {
          return;
        }
        setDestinationValidation({
          status: "success",
          customerName: result.customerName || "Unknown customer",
          accountNumber: normalizedToAccountNumber,
          message: "Destination account verified",
        });
      } catch (err) {
        if (cancelled) {
          return;
        }
        setDestinationValidation({
          status: "error",
          customerName: "",
          accountNumber: normalizedToAccountNumber,
          message: err.message || "Could not validate destination account",
        });
      }
    }, 300);

    return () => {
      cancelled = true;
      clearTimeout(timeoutId);
    };
  }, [showTransferForm, transferForm.fromAccountId, normalizedToAccountNumber, hasValidToAccountFormat]);

  useEffect(() => {
    if (!showTransferForm && !showLocalBankForm) {
      return;
    }
    if (!sourceAccount) {
      if (transferForm.fromAccountId) {
        setTransferForm((prev) => ({ ...prev, fromAccountId: "" }));
      }
      return;
    }
    if (String(transferForm.fromAccountId || "") !== String(sourceAccount.id)) {
      setTransferForm((prev) => ({ ...prev, fromAccountId: String(sourceAccount.id) }));
    }
  }, [showTransferForm, showLocalBankForm, sourceAccount, transferForm.fromAccountId, setTransferForm]);

  async function handleSendTransfer(e) {
    e.preventDefault();

    if (!sourceAccount) {
      setDestinationValidation({
        status: "error",
        customerName: "",
        accountNumber: "",
        message: "Please select a valid source account",
      });
      return;
    }

    if (!normalizedToAccountNumber) {
      setDestinationValidation({
        status: "error",
        customerName: "",
        accountNumber: "",
        message: "Destination account number or customer ID is required",
      });
      return;
    }

    if (!hasValidToAccountFormat) {
      setDestinationValidation({
        status: "error",
        customerName: "",
        accountNumber: normalizedToAccountNumber,
        message: "Enter a 12-digit account number or numeric customer ID",
      });
      return;
    }

    try {
      setDestinationValidation({
        status: "loading",
        customerName: "",
        accountNumber: normalizedToAccountNumber,
        message: "Validating destination account...",
      });

      const result = await api.validateTransferDestination({
        fromAccountId: Number(transferForm.fromAccountId || sourceAccount.id),
        toAccountNumber: normalizedToAccountNumber,
      });

      const validatedName = result.customerName || "Unknown customer";
      setDestinationValidation({
        status: "success",
        customerName: validatedName,
        accountNumber: normalizedToAccountNumber,
        message: "Destination account verified",
      });

      try {
        await onInitiateTransfer();
      } catch (transferErr) {
        const rawMessage = String(transferErr?.message || "").trim();
        const friendlyMessage =
          !rawMessage || /Cannot read properties of undefined/i.test(rawMessage)
            ? "Destination verified, but transfer could not be completed. Please try again."
            : rawMessage;

        setDestinationValidation({
          status: "error",
          customerName: "",
          accountNumber: normalizedToAccountNumber,
          message: friendlyMessage,
        });
      }
    } catch (err) {
      setDestinationValidation({
        status: "error",
        customerName: "",
        accountNumber: normalizedToAccountNumber,
        message: err.message || "Could not validate destination account",
      });
    }
  }

  function handleLocalBankSubmit(e) {
    e.preventDefault();
    setLocalBankMessage("Local bank transfer submitted. Processing may take 1\u20133 business days.");
  }

  return (
    <section className="panel-grid">
      <article className="panel wide">
        {/* Horizontal transfer type tab bar */}
        <nav className="acct-tab-bar">
          {TRANSFER_OPTIONS.map((option) => (
            <button
              key={option.id}
              type="button"
              className={`acct-tab-btn${option.id === activeOption ? " active" : ""}`}
              onClick={() => setActiveOption(option.id)}
            >
              {option.label}
            </button>
          ))}
        </nav>

        <div className="acct-tab-body">
          <p className="hint transfers-tab-desc">{activeTransferOption.description}</p>

        {showTransferForm ? (
          <form onSubmit={handleSendTransfer}>
            <label>
              To Account / Customer ID
              <input
                value={transferForm.toAccountNumber || ""}
                onChange={(e) => setTransferForm({ ...transferForm, toAccountNumber: e.target.value })}
                placeholder="Enter 12-digit account number or customer ID"
                required
              />
            </label>
            {normalizedToAccountNumber ? (
              <p className={destinationValidation.status === "success" ? "hint" : "status error"}>
                {destinationValidation.status === "success"
                  ? `Customer Name: ${destinationValidation.customerName}`
                  : destinationValidation.message}
              </p>
            ) : null}
            <label>
              Amount (FJD)
              <input
                type="number"
                min="1"
                step="0.01"
                value={transferForm.amount}
                onChange={(e) => setTransferForm({ ...transferForm, amount: e.target.value })}
                required
              />
            </label>
            <label>
              Reason of Transfer
              <input
                value={transferForm.description || ""}
                onChange={(e) => setTransferForm({ ...transferForm, description: e.target.value })}
                placeholder="Enter transfer reason"
              />
            </label>
            <button type="submit" disabled={!sourceAccount}>
              Send Transfer
            </button>
            {transferMessage ? (
              <p className={/success|verified|completed|pending/i.test(String(transferMessage)) ? "hint" : "status error"}>
                {transferMessage}
              </p>
            ) : null}
          </form>
        ) : showLocalBankForm ? (
          <form onSubmit={handleLocalBankSubmit}>
            <label>
              Recipient Name
              <input
                value={localBankForm.recipientName}
                onChange={(e) => setLocalBankForm({ ...localBankForm, recipientName: e.target.value })}
                placeholder="Enter recipient full name"
                required
              />
            </label>
            <label>
              Destination Account Number
              <input
                value={localBankForm.accountNumber}
                onChange={(e) => setLocalBankForm({ ...localBankForm, accountNumber: e.target.value })}
                placeholder="Enter destination account number"
                required
              />
            </label>
            <label>
              Bank
              <select
                value={localBankForm.bankName}
                onChange={(e) => setLocalBankForm({ ...localBankForm, bankName: e.target.value })}
                required
              >
                <option value="" disabled>Select a bank</option>
                {FIJI_LOCAL_BANKS.map((bank) => (
                  <option key={bank} value={bank}>{bank}</option>
                ))}
              </select>
            </label>
            <label>
              Amount (FJD)
              <input
                type="number"
                min="1"
                step="0.01"
                value={localBankForm.amount}
                onChange={(e) => setLocalBankForm({ ...localBankForm, amount: e.target.value })}
                required
              />
            </label>
            <label>
              Reason of Transfer
              <input
                value={localBankForm.description}
                onChange={(e) => setLocalBankForm({ ...localBankForm, description: e.target.value })}
                placeholder="Enter transfer reason"
              />
            </label>
            {localBankMessage && <p className="hint">{localBankMessage}</p>}
            <button type="submit" disabled={!sourceAccount}>
              Send Transfer
            </button>
          </form>
        ) : showLimitsContent ? (
          <div className="transfers-placeholder">
            <p className="hint">Current entered amount: FJD {currentTransferAmount.toLocaleString()}</p>
            {limitRows.map((row) => {
              const percent = Math.min(100, Math.round((currentTransferAmount / row.value) * 100));
              return (
                <div key={row.label}>
                  <p className="metric">
                    {row.label} Limit: FJD {row.value.toLocaleString()} ({percent}%)
                  </p>
                  <div
                    style={{
                      width: "100%",
                      height: "10px",
                      background: "#dfe9f6",
                      borderRadius: "999px",
                      overflow: "hidden",
                      marginBottom: "10px",
                    }}
                  >
                    <div
                      style={{
                        width: `${percent}%`,
                        height: "100%",
                        background: "linear-gradient(90deg, #0f6bcf, #37c0a0)",
                      }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        ) : showWalletContent ? (
          <div className="transfers-placeholder">
            <p className="hint">Supported digital wallets in Fiji:</p>
            <label>
              Select Wallet Provider
              <select defaultValue="">
                <option value="" disabled>
                  Choose digital wallet
                </option>
                {FIJI_DIGITAL_WALLETS.map((wallet) => (
                  <option key={wallet.name} value={wallet.name}>
                    {wallet.name} - {wallet.provider}
                  </option>
                ))}
              </select>
            </label>
          </div>
        ) : (
          <div className="transfers-placeholder">
            <p className="hint">
              Select a transfer option from the menu.
            </p>
          </div>
        )}
        </div>
      </article>
    </section>
  );
}
