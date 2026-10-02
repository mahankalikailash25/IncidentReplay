import React from 'react';
import { useCursorGlow } from '../../hooks/useCursorGlow';

export function Card({ children, className = '', interactive = false, ...props }) {
  const glowRef = useCursorGlow();

  const baseClasses = 'surface cursor-glow-wrapper';
  const interactiveClasses = interactive ? 'interactive' : '';
  const combinedClasses = `${baseClasses} ${interactiveClasses} ${className}`.trim();

  return (
    <div ref={glowRef} className={combinedClasses} {...props}>
      {children}
    </div>
  );
}

export function CardHeader({ children, className = '' }) {
  return <div className={`flex-between mb-4 ${className}`}>{children}</div>;
}

export function CardTitle({ children, className = '' }) {
  return <h3 className={`text-lg font-semibold m-0 ${className}`}>{children}</h3>;
}

export function CardBody({ children, className = '' }) {
  return <div className={className}>{children}</div>;
}
