import type { Metadata } from 'next';
import '../globals.css';
import Providers from './providers';

export const metadata: Metadata = {
  title: 'SignalScout - Investment Research & Discovery',
  description: 'AI-powered investment research platform for analyzing companies and generating buy/sell/hold recommendations',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
