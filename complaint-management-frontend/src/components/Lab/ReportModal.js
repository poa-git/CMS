import React from "react";
import "./ReportModal.css";
import ReportList from "./ReportList";

const ReportModal = ({ isOpen, complaintId, handleClose, allowAdd, onReportAdded }) => {
  if (!isOpen) return null;

  return (
    <div className="report-modal-overlay">
      <div className="report-modal">
        <button className="report-modal-close" onClick={handleClose}>
          &times;
        </button>
        <h2>Hardware Reports</h2>
        <ReportList
          complaintId={complaintId}
          allowAdd={allowAdd}
          onReportAdded={() => {
            if (onReportAdded) onReportAdded(complaintId);
          }}
        />
      </div>
    </div>
  );
};

export default ReportModal;
