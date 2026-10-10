import React from 'react';

export default function ResourceCard({ title, value, meta, tone = 'default' }) {
  return (
    <div className={`resource-card resource-card-${tone}`}>
      <span className="card-label">{title}</span>
      <strong className="card-value">{value}</strong>
      {meta ? <span className="card-meta">{meta}</span> : null}
    </div>
  );
}
