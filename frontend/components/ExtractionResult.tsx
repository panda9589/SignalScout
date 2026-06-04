import { DocumentExtractedResponse } from '@/lib/types';
import { AiOutlineRise, AiOutlineWarning } from 'react-icons/ai';

interface ExtractionResultProps {
  result: DocumentExtractedResponse;
  onClose: () => void;
}

export default function ExtractionResult({ result, onClose }: ExtractionResultProps) {
  const recommendationColor = (recommendation: string) => {
    switch (recommendation) {
      case 'BUY':
        return 'text-green-600 bg-green-50';
      case 'WATCH':
        return 'text-blue-600 bg-blue-50';
      case 'HOLD':
        return 'text-gray-600 bg-gray-50';
      case 'AVOID':
        return 'text-red-600 bg-red-50';
      default:
        return 'text-gray-600 bg-gray-50';
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-lg max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        <div className="sticky top-0 bg-white border-b p-6 flex justify-between items-start">
          <div>
            <h2 className="text-2xl font-bold">{result.ticker}</h2>
            <p className="text-gray-600">{result.companyName}</p>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 text-2xl"
            aria-label="Close"
          >
            x
          </button>
        </div>

        <div className="p-6 space-y-6">
          <div className={`p-4 rounded-lg ${recommendationColor(result.recommendation)}`}>
            <div className="font-bold text-lg">{result.recommendation}</div>
            <div className="text-sm">Stock Score: {result.stockScore}/100</div>
          </div>

          <div className="border-t pt-4">
            <h3 className="font-bold text-lg mb-2">Event Summary</h3>
            <p className="text-gray-700 mb-3">{result.summary}</p>
            {result.whatChanged && (
              <div className="bg-gray-50 p-3 rounded text-sm text-gray-600">
                <strong>What Changed:</strong> {result.whatChanged}
              </div>
            )}
          </div>

          <div className="grid grid-cols-2 gap-4 border-t pt-4">
            <div>
              <div className="text-sm text-gray-600">Bullish Score</div>
              <div className="text-2xl font-bold text-green-600">{result.bullishScore}</div>
            </div>
            <div>
              <div className="text-sm text-gray-600">Bearish Score</div>
              <div className="text-2xl font-bold text-red-600">{result.bearishScore}</div>
            </div>
            <div>
              <div className="text-sm text-gray-600">Source Quality</div>
              <div className="text-2xl font-bold text-blue-600">{result.sourceQualityScore}</div>
            </div>
            <div>
              <div className="text-sm text-gray-600">Confidence</div>
              <div className="text-2xl font-bold text-purple-600">{result.confidenceScore}</div>
            </div>
          </div>

          {result.bullCase && result.bullCase.length > 0 && (
            <div className="border-t pt-4">
              <h3 className="font-bold text-green-700 mb-2 flex items-center">
                <AiOutlineRise className="mr-2" /> Bull Case
              </h3>
              <ul className="space-y-1">
                {result.bullCase.map((point, idx) => (
                  <li key={idx} className="text-sm text-gray-700">
                    - {point}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {result.bearCase && result.bearCase.length > 0 && (
            <div className="border-t pt-4">
              <h3 className="font-bold text-red-700 mb-2 flex items-center">
                <AiOutlineWarning className="mr-2" /> Bear Case
              </h3>
              <ul className="space-y-1">
                {result.bearCase.map((point, idx) => (
                  <li key={idx} className="text-sm text-gray-700">
                    - {point}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {result.risks && result.risks.length > 0 && (
            <div className="border-t pt-4">
              <h3 className="font-bold text-orange-700 mb-2">Risks</h3>
              <ul className="space-y-1">
                {result.risks.map((risk, idx) => (
                  <li key={idx} className="text-sm text-gray-700">
                    - {risk}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {result.watchItems && result.watchItems.length > 0 && (
            <div className="border-t pt-4">
              <h3 className="font-bold text-blue-700 mb-2">Watch Items</h3>
              <ul className="space-y-1">
                {result.watchItems.map((item, idx) => (
                  <li key={idx} className="text-sm text-gray-700">
                    - {item}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {result.requiresManualReview && (
            <div className="bg-yellow-50 border border-yellow-200 p-4 rounded">
              <p className="text-sm text-yellow-800">
                <strong>Manual Review Required:</strong> {result.manualReviewReason}
              </p>
            </div>
          )}

          <div className="border-t pt-4">
            <button
              onClick={onClose}
              className="w-full bg-blue-600 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
            >
              Close
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
