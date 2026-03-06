import React, { useState, useEffect } from "react";
import axios from "axios";
import "./ReportList.css"; // Make sure the CSS file is up to date

const ReportList = ({ complaintId, allowAdd, onReportAdded }) => {
  const [reports, setReports] = useState([]);
  const [newReport, setNewReport] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    if (complaintId) {
      fetchReports();
    }
    // eslint-disable-next-line
  }, [complaintId]);

  const fetchReports = async () => {
    try {
      setIsLoading(true);
      const response = await axios.get(
        `${process.env.REACT_APP_API_BASE_URL}/hardware-logs/${complaintId}/reports`,
        { withCredentials: true }
      );
      setReports(response.data);
    } catch (error) {
      console.error("Error fetching reports:", error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleAddReport = async () => {
    if (!newReport.trim()) return;
    try {
      const payload = { content: newReport, createdBy: "currentUser" };
      const response = await axios.post(
        `${process.env.REACT_APP_API_BASE_URL}/hardware-logs/${complaintId}/reports`,
        payload,
        { withCredentials: true }
      );
      setReports([...reports, response.data]);
      setNewReport("");
      if (onReportAdded) {
        onReportAdded(); // 🔔 Notify parent
      }
    } catch (error) {
      console.error("Error adding report:", error);
    }
  };

  // Sort oldest to newest (newest at the end)
  const sortedReports = [...reports].sort((a, b) =>
    new Date(a.createdAt) - new Date(b.createdAt)
  );

  return (
    <div className="report-container">
      <h3 className="report-title">Hardware Reports</h3>

      {isLoading && <p className="report-loading">Loading reports...</p>}

      {!isLoading && reports.length === 0 && (
        <p className="report-empty">No reports found for this hardware.</p>
      )}
      <ul className="report-list">
        {sortedReports.map((report) => (
          <li key={report.id} className="report-item">
            <div className="report-row-header">
              <span className="report-date">
                {report.createdAt
                  ? new Date(report.createdAt).toLocaleString()
                  : ""}
              </span>
              <span className="report-createdby">
                <svg
                  width="12"
                  height="12"
                  style={{ marginRight: 4, verticalAlign: "middle" }}
                  fill="currentColor"
                  viewBox="0 0 16 16"
                >
                  <path d="M8 8a3 3 0 1 0 0-6 3 3 0 0 0 0 6zm4-3a4 4 0 1 1-8 0 4 4 0 0 1 8 0zM2 14s-1 0-1-1 1-4 7-4 7 3 7 4-1 1-1 1H2zm13-1c0-.242-.444-2-6-2s-6 1.758-6 2c0 .547.632 1 1 1h10c.368 0 1-.453 1-1z" />
                </svg>
                {report.createdBy}
              </span>
            </div>
            <div className="report-row-content">{report.content}</div>
          </li>
        ))}
      </ul>
      {allowAdd && (
        <div className="report-form">
          <label htmlFor="newReport" className="report-label">
            Add New Report
          </label>
          <textarea
            id="newReport"
            value={newReport}
            onChange={(e) => setNewReport(e.target.value)}
            rows="3"
            className="report-textarea"
            placeholder="Write your report here…"
          />
          <button onClick={handleAddReport} className="report-button">
            Submit Report
          </button>
        </div>
      )}
    </div>
  );
};

export default ReportList;
