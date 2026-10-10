export const categoryDetails = {
  RED: { label: 'Immediate', responseTime: '0 min', color: '#d92d20' },
  ORANGE: { label: 'Very urgent', responseTime: '10 min', color: '#e67e22' },
  YELLOW: { label: 'Urgent', responseTime: '60 min', color: '#c58b00' },
  GREEN: { label: 'Standard', responseTime: '120 min', color: '#14804a' },
  BLUE: { label: 'Non-urgent', responseTime: '240 min', color: '#2563eb' },
};

export const categoryNames = Object.keys(categoryDetails);
