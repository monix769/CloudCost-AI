import React, { useState, useRef } from 'react';
import RecentBillingTable from '../components/RecentBillingTable';
import billingService from '../services/billingService';
import { UploadCloud, CheckCircle, AlertCircle, Loader2 } from 'lucide-react';

export default function BillingManagementPage() {
  const [uploading, setUploading] = useState(false);
  const [uploadResult, setUploadResult] = useState(null);
  const [uploadError, setUploadError] = useState(null);
  const [refreshTrigger, setRefreshTrigger] = useState(0);
  const fileInputRef = useRef(null);

  const handleFileChange = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      setUploading(true);
      setUploadError(null);
      setUploadResult(null);

      const res = await billingService.uploadBillingCsv(file);
      setUploadResult(res);
      setRefreshTrigger((prev) => prev + 1);
    } catch (err) {
      setUploadError(err.message || 'Failed to upload and parse CSV billing file');
    } finally {
      setUploading(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800">
        <div>
          <h2 className="text-xl font-bold text-white flex items-center space-x-2">
            <span>Billing Records &amp; Ingestion</span>
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Persisted MySQL billing records, RFC-4180 CSV parser, and line-item expenditure activity
          </p>
        </div>

        <div>
          <input
            type="file"
            ref={fileInputRef}
            onChange={handleFileChange}
            accept=".csv"
            className="hidden"
            id="csv-upload-input"
          />
          <label
            htmlFor="csv-upload-input"
            className={`cursor-pointer inline-flex items-center space-x-2 px-4 py-2 rounded-xl text-xs font-semibold bg-blue-600 hover:bg-blue-500 text-white shadow-lg shadow-blue-500/20 transition-all ${
              uploading ? 'opacity-70 pointer-events-none' : ''
            }`}
          >
            {uploading ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <UploadCloud className="w-4 h-4" />
            )}
            <span>{uploading ? 'Processing CSV...' : 'Upload Billing CSV'}</span>
          </label>
        </div>
      </div>

      {/* Upload result alert */}
      {uploadResult && (
        <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 text-xs space-y-1">
          <div className="flex items-center space-x-2 font-semibold">
            <CheckCircle className="w-4 h-4" />
            <span>CSV Upload Complete</span>
          </div>
          <p className="font-mono">{uploadResult.summary}</p>
        </div>
      )}

      {/* Upload error alert */}
      {uploadError && (
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs flex items-center space-x-2">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>{uploadError}</span>
        </div>
      )}

      {/* Itemized Billing Table */}
      <RecentBillingTable refreshTrigger={refreshTrigger} />
    </div>
  );
}
