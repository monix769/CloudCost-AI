import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  ArcElement,
  Title,
  Tooltip,
  Legend,
  Filler,
} from 'chart.js';

// Register all required Chart.js controllers, elements, scales, and plugins
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  ArcElement,
  Title,
  Tooltip,
  Legend,
  Filler
);

// Global default styling for Dark Navy theme
ChartJS.defaults.color = '#94a3b8';
ChartJS.defaults.font.family = 'Inter, system-ui, sans-serif';
ChartJS.defaults.font.size = 11;

export const commonChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      labels: {
        color: '#cbd5e1',
        boxWidth: 12,
        boxHeight: 12,
        borderRadius: 3,
        usePointStyle: true,
        font: {
          family: 'Inter, sans-serif',
          size: 11,
          weight: '500',
        },
      },
    },
    tooltip: {
      backgroundColor: '#0b1120',
      titleColor: '#f8fafc',
      bodyColor: '#93c5fd',
      borderColor: 'rgba(59, 130, 246, 0.3)',
      borderWidth: 1,
      padding: 10,
      boxPadding: 4,
      usePointStyle: true,
      callbacks: {
        label: function (context) {
          let label = context.dataset.label || '';
          if (label) {
            label += ': ';
          }
          if (context.parsed.y !== null && context.parsed.y !== undefined) {
            label += new Intl.NumberFormat('en-US', {
              style: 'currency',
              currency: 'USD',
              maximumFractionDigits: 0,
            }).format(context.parsed.y);
          } else if (context.parsed !== null && context.parsed !== undefined) {
            label += new Intl.NumberFormat('en-US', {
              style: 'currency',
              currency: 'USD',
              maximumFractionDigits: 0,
            }).format(context.parsed);
          }
          return label;
        },
      },
    },
  },
  scales: {
    x: {
      grid: {
        color: 'rgba(255, 255, 255, 0.04)',
        drawBorder: false,
      },
      ticks: {
        color: '#64748b',
        font: {
          size: 11,
        },
      },
    },
    y: {
      grid: {
        color: 'rgba(255, 255, 255, 0.04)',
        drawBorder: false,
      },
      ticks: {
        color: '#64748b',
        font: {
          size: 11,
        },
        callback: function (value) {
          if (value >= 1000) {
            return '$' + (value / 1000).toFixed(0) + 'k';
          }
          return '$' + value;
        },
      },
    },
  },
};
