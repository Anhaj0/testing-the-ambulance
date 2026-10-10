import { useState } from 'react';

const initialFormData = {
  patientId: '',
  breathing: true,
  pulseRate: 80,
  avpu: 'ALERT',
  oxygenSaturation: 98,
  systolicBP: 120,
  painScore: 2,
  temperature: 37,
  age: 30,
  hazardPresent: false,
  symptoms: '',
};

const numericFields = new Set([
  'patientId', 'pulseRate', 'oxygenSaturation', 'systolicBP', 'painScore', 'temperature', 'age',
]);

function NumberField({ label, name, value, unit, min, max, step = 1, onChange, readOnly = false }) {
  return (
    <label className="triage-field">
      <span>{label}</span>
      <div className="triage-input-unit">
        <input name={name} type="number" value={value} min={min} max={max} step={step} onChange={onChange} readOnly={readOnly} required />
        {unit && <small>{unit}</small>}
      </div>
    </label>
  );
}

export default function TriageForm({ isSubmitting, onSubmit, patient, disabled = false }) {
  const [formData, setFormData] = useState(() => ({
    ...initialFormData, patientId: patient?.id ?? '', age: patient?.age ?? initialFormData.age,
  }));

  function handleChange(event) {
    const { checked, name, type, value } = event.target;
    setFormData((current) => ({
      ...current,
      [name]: type === 'checkbox' ? checked : (numericFields.has(name) && value === '' ? '' : value),
    }));
  }

  function handleSubmit(event) {
    event.preventDefault();
    onSubmit({
      ...formData,
      patientId: Number(formData.patientId),
      pulseRate: Number(formData.pulseRate),
      oxygenSaturation: Number(formData.oxygenSaturation),
      systolicBP: Number(formData.systolicBP),
      painScore: Number(formData.painScore),
      temperature: Number(formData.temperature),
      age: Number(formData.age),
      symptoms: formData.symptoms.split(',').map((item) => item.trim()).filter(Boolean),
    });
  }

  return (
    <form className="triage-form" onSubmit={handleSubmit}>
      <fieldset className="fieldset-plain" disabled={isSubmitting || disabled}>
      <div className="triage-section-heading">
        <div>
          <span>01</span>
          <h2>Patient assessment</h2>
        </div>
        <p>All clinical fields are required.</p>
      </div>

      {patient && <p className="empty-note">Assessing {patient.name}. Choose a different emergency call to change patient.</p>}
      <NumberField label="Patient ID" name="patientId" value={formData.patientId} min="1" onChange={handleChange} readOnly={Boolean(patient)} />

      <div className="triage-toggle-grid">
        <label className={`triage-toggle ${formData.breathing ? 'is-positive' : 'is-critical'}`}>
          <input name="breathing" type="checkbox" checked={formData.breathing} onChange={handleChange} />
          <span><strong>Breathing</strong><small>{formData.breathing ? 'Spontaneous' : 'Not breathing'}</small></span>
        </label>
        <label className={`triage-toggle ${formData.hazardPresent ? 'is-critical' : ''}`}>
          <input name="hazardPresent" type="checkbox" checked={formData.hazardPresent} onChange={handleChange} />
          <span><strong>Scene hazard</strong><small>{formData.hazardPresent ? 'Hazard present' : 'Scene secure'}</small></span>
        </label>
      </div>

      <div className="triage-field-grid">
        <NumberField label="Pulse rate" name="pulseRate" value={formData.pulseRate} unit="bpm" min="0" max="300" onChange={handleChange} />
        <NumberField label="Oxygen saturation" name="oxygenSaturation" value={formData.oxygenSaturation} unit="%" min="0" max="100" onChange={handleChange} />
        <NumberField label="Systolic pressure" name="systolicBP" value={formData.systolicBP} unit="mmHg" min="0" max="300" onChange={handleChange} />
        <NumberField label="Temperature" name="temperature" value={formData.temperature} unit="°C" min="20" max="45" step="0.1" onChange={handleChange} />
        <NumberField label="Pain score" name="painScore" value={formData.painScore} unit="/ 10" min="0" max="10" onChange={handleChange} />
        <NumberField label="Patient age" name="age" value={formData.age} unit="years" min="0" max="130" onChange={handleChange} />
        <label className="triage-field triage-wide"><span>Consciousness level (AVPU)</span>
          <select name="avpu" value={formData.avpu} onChange={handleChange}>
            <option value="ALERT">Alert</option><option value="VOICE">Responds to voice</option>
            <option value="PAIN">Responds to pain</option><option value="UNRESPONSIVE">Unresponsive</option>
          </select>
        </label>
        <label className="triage-field triage-wide"><span>Symptoms</span>
          <textarea name="symptoms" value={formData.symptoms} onChange={handleChange} rows="3" placeholder="Chest pain, shortness of breath, dizziness" />
          <small>Separate multiple symptoms with commas.</small>
        </label>
      </div>
      <button className="triage-submit" type="submit" disabled={isSubmitting || disabled}>{isSubmitting ? 'Evaluating patient…' : 'Evaluate and prioritize'}</button>
      </fieldset>
    </form>
  );
}
