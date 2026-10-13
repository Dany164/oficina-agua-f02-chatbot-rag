import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'Asistente virtual · Oficina de Agua',
  description:
      'Asistente virtual de la Oficina de Agua: consulta horarios, requisitos, tarifas, pagos y reportes de fugas.',
};

export default function RootLayout({ children }: LayoutProps<'/'>) {
  return (
      <html lang="es">
      <body className="antialiased">{children}</body>
      </html>
  );
}