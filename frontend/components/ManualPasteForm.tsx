import { useState } from 'react';
import toast from 'react-hot-toast';
import { apiClient } from '@/lib/api-client';
import { DocumentExtractedResponse } from '@/lib/types';
import { AiOutlineLoading3Quarters } from 'react-icons/ai';

interface ManualPasteFormProps {
  companyId: number;
  companyTicker: string;
  onSuccess: (result: DocumentExtractedResponse) => void;
}

export default function ManualPasteForm({
  companyId,
  companyTicker,
  onSuccess,
}: ManualPasteFormProps) {
  const [text, setText] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!text.trim()) {
      toast.error('Please paste some text');
      return;
    }

    setLoading(true);
    try {
      const result = await apiClient.pasteDocument({
        rawText: text,
        companyId: companyId.toString(),
        sourceType: 'Manual',
      });

      toast.success('Document extracted successfully!');
      setText('');
      onSuccess(result);
    } catch (error: any) {
      const message =
        error.response?.data?.message || error.message || 'Failed to extract document';
      toast.error(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="block text-sm font-semibold text-gray-700 mb-2">
          Paste Document Text
        </label>
        <textarea
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder="Paste earnings transcript, news article, or any investment research document..."
          className="w-full h-40 p-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
          disabled={loading}
        />
      </div>

      <button
        type="submit"
        disabled={loading}
        className="w-full bg-blue-600 hover:bg-blue-700 disabled:bg-gray-400 text-white font-bold py-2 px-4 rounded transition flex items-center justify-center gap-2"
      >
        {loading && <AiOutlineLoading3Quarters className="animate-spin" />}
        {loading ? 'Processing...' : 'Extract Investment Event'}
      </button>

      <p className="text-xs text-gray-500">
        This will analyze the text using AI to extract investment events, scores, and recommendations.
      </p>
    </form>
  );
}
