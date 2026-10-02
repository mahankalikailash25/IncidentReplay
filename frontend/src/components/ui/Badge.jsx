import React from 'react';

export function Badge({ 
  children, 
  variant = 'neutral', 
  className = '' 
}) {
  const baseClasses = 'badge';
  const variantClasses = `badge-${variant}`;
  const combinedClasses = `${baseClasses} ${variantClasses} ${className}`.trim();

  return (
    <span className={combinedClasses}>
      {children}
    </span>
  );
}
