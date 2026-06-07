import { ExtractedEvent } from '@/lib/types';
import { format } from 'date-fns';

interface EventCardProps {
  event: ExtractedEvent;
}

export default function EventCard({ event }: EventCardProps) {
  const eventTypeLabel = event.eventType
    .split('_')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');

  const sentimentColor =
    event.bullishScore > event.bearishScore
      ? 'border-green-200 bg-green-50'
      : event.bearishScore > event.bullishScore
      ? 'border-red-200 bg-red-50'
      : 'border-gray-200 bg-gray-50';

  return (
    <div className={`border rounded-lg p-4 ${sentimentColor}`}>
      <div className="flex justify-between items-start mb-2">
        <div>
          <div className="font-bold text-sm text-blue-600">{eventTypeLabel}</div>
          {event.eventDate && (
            <div className="text-xs text-gray-500">
              {format(new Date(event.eventDate), 'MMM d, yyyy')}
            </div>
          )}
        </div>
        <div className="text-right">
          <div className="text-xs font-semibold">
            <span className="text-green-600">{event.bullishScore}</span>
            <span className="text-gray-400"> / </span>
            <span className="text-red-600">{event.bearishScore}</span>
          </div>
          <div className="text-xs text-gray-500">Confidence: {event.confidenceScore}%</div>
        </div>
      </div>

      <p className="text-sm text-gray-700 mb-3">{event.summary}</p>

      <div className="grid grid-cols-2 gap-2 text-xs">
        <div className="bg-white/50 p-2 rounded">
          <span className="text-gray-600">Source Quality: </span>
          <span className="font-semibold">{event.sourceQualityScore}</span>
        </div>
        {event.requiresManualReview && (
          <div className="bg-yellow-100 p-2 rounded text-yellow-800 font-semibold">
            ⚠️ Review Needed
          </div>
        )}
      </div>
    </div>
  );
}
