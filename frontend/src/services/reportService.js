import apiClient from './api';

export const reportService = {
  /**
   * Request generation of a new report (PDF or CSV).
   * @param {Object} payload - { reportType, format, startDate, endDate, provider, currency }
   */
  async generateReport(payload) {
    const response = await apiClient.post('/api/reports/generate', payload);
    return response.data?.data;
  },

  /**
   * List report history with optional filtering.
   * @param {Object} params - { type, page, size }
   */
  async getReports(params = {}) {
    const response = await apiClient.get('/api/reports', { params });
    return response.data?.data;
  },

  /**
   * Get single report metadata.
   * @param {number|string} id
   */
  async getReportById(id) {
    const response = await apiClient.get(`/api/reports/${id}`);
    return response.data?.data;
  },

  /**
   * Download a generated report file directly to client machine.
   * @param {number|string} id - Report ID
   * @param {string} fallbackFilename - Optional fallback name
   */
  async downloadReport(id, fallbackFilename = 'report') {
    const response = await apiClient.get(`/api/reports/${id}/download`, {
      responseType: 'blob',
    });

    // Try to get filename from Content-Disposition header if available
    let filename = fallbackFilename;
    const disposition = response.headers['content-disposition'];
    if (disposition && disposition.indexOf('filename=') !== -1) {
      const match = disposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
      if (match != null && match[1]) {
        filename = match[1].replace(/['"]/g, '');
      }
    }

    const blob = new Blob([response.data], {
      type: response.headers['content-type'] || 'application/octet-stream',
    });
    const downloadUrl = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(downloadUrl);

    return true;
  },

  /**
   * Delete a generated report file and record.
   * @param {number|string} id
   */
  async deleteReport(id) {
    const response = await apiClient.delete(`/api/reports/${id}`);
    return response.data;
  },
};

export default reportService;

