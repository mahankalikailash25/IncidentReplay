import React from 'react';

export function Button({ 
  children, 
  variant = 'primary', 
  className = '', 
  icon: Icon,
  loading = false,
  ...props 
}) {
  const baseClasses = 'btn';
  const variantClasses = `btn-${variant}`;
  const combinedClasses = `${baseClasses} ${variantClasses} ${loading ? 'loading' : ''} ${className}`.trim();

  return (
    <button className={combinedClasses} disabled={loading || props.disabled} {...props}>
      {!loading && Icon && <Icon size={16} />}
      {children}
    </button>
  );
}
