import React, {
  useState,
  useEffect,
  useRef,
  useCallback,
} from "react";
import axios from "axios";
import ComplaintReport from "../../ComplaintReport/ComplaintReport";
import "./ComplaintTable.css";
import ReportModal from "../../Lab/ReportModal";
import ComplaintFilters from "../ComplaintFilters/ComplaintFilters";
import { generateExcelReport } from "../../ExcelReportGenerator/ExcelReportGenerator";
import { generateDaySummaryReport } from "../../ExcelReportGenerator/generateDaySummaryReport";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import Loader from "../../../utils/Loader";
import { useFilters } from "../../../context/FiltersContext";

// WebSocket live hook
function useComplaintReportsLive(onUpdate) {
  useEffect(() => {
    const wsUrl = `${process.env.REACT_APP_API_BASE_URL || ""}/ws`;

    const client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: () => {},

      onConnect: () => {
        console.log("WebSocket connected");

        client.subscribe("/topic/paginated-by-status", (message) => {
          try {
            const data =
              typeof message.body === "string"
                ? JSON.parse(message.body)
                : message.body;

            console.log("WS message:", data);
            onUpdate?.(data);
          } catch (err) {
            console.error("WS parse error:", err);
          }
        });
      },

      onStompError: (frame) => {
        console.error("STOMP error:", frame);
      },

      onWebSocketClose: (event) => {
        console.warn("WebSocket closed:", event);
      },

      onWebSocketError: (event) => {
        console.error("WebSocket error:", event);
      },
    });

    client.activate();

    return () => {
      try {
        client.deactivate();
      } catch (err) {
        console.error("WebSocket deactivate error:", err);
      }
    };
  }, [onUpdate]);
}

const pageSize = 80;

const OpenComplaintsTable = ({
  handleOpenModal,
  openRemarksModal,
  calculateAgingDays,
  getStatusClass,
  getCourierStatusClass,
  complaintsRefreshKey,
  fetchDashboardCounts,
}) => {
  const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;

  const { filters: globalFilters, setFilters, defaultFilters } = useFilters();
  const [status] = useState("Open");

  const [remarksCounts, setRemarksCounts] = useState({});
  const [groups, setGroups] = useState([]);
  const [totalPages, setTotalPages] = useState(1);
  const [currentPage, setCurrentPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [bankList, setBankList] = useState([]);
  const [cityList, setCityList] = useState([]);
  const [statusList, setStatusList] = useState([]);
  const [engineers, setEngineers] = useState([]);
  const [totalRecords, setTotalRecords] = useState(0);
  const [progress, setProgress] = useState(0);
  const [selectedComplaintId, setSelectedComplaintId] = useState(null);
  const [selectedRow, setSelectedRow] = useState(null);
  const [hoveredComplaintId, setHoveredComplaintId] = useState(null);
  const [selectedComplaintIdForReports, setSelectedComplaintIdForReports] =
    useState(null);
  const [isReportModalOpen, setIsReportModalOpen] = useState(false);
  const [reportAvailability, setReportAvailability] = useState({});
  const [complaintsBeforePage, setComplaintsBeforePage] = useState(0);

  // Keep latest groups in ref for websocket callback
  const groupsRef = useRef(groups);
  useEffect(() => {
    groupsRef.current = groups;
  }, [groups]);

  const allComplaintsFlat = groups.flatMap((g) => g.complaints || []);

  const uniqueBanks = Array.from(
    new Set(allComplaintsFlat.map((c) => c.bankName).filter(Boolean))
  );
  const uniqueEngineers = Array.from(
    new Set(allComplaintsFlat.map((c) => c.engineerName).filter(Boolean))
  );
  const uniqueCities = Array.from(
    new Set(allComplaintsFlat.map((c) => c.city).filter(Boolean))
  );
  const uniqueSubStatuses = Array.from(
    new Set(allComplaintsFlat.map((c) => c.subStatus).filter(Boolean))
  );

  const reportTypes = [
    { value: "standard", label: "Standard" },
    { value: "untouched", label: "Untouched" },
    { value: "ageing", label: "Aging" },
    { value: "daySummaryMulti", label: "Day Summary" },
  ];

  const pick = (obj, keys) =>
    Object.fromEntries(
      keys
        .map((k) => [k, obj?.[k]])
        .filter(([, v]) => v !== undefined && v !== "")
    );

  const OPEN_ALLOWED_KEYS = [
    "bankName",
    "branchCode",
    "branchName",
    "engineerName",
    "city",
    "complaintStatus",
    "subStatus",
    "priority",
    "inPool",
    "hasReport",
    "date",
    "dateFrom",
    "dateTo",
    "reportType",
  ];

  const fetchComplaints = useCallback(async () => {
    setLoading(true);
    setProgress(0);

    let interval = setInterval(() => {
      setProgress((prev) => (prev < 90 ? prev + 10 : prev));
    }, 300);

    try {
      const shared = pick(globalFilters, OPEN_ALLOWED_KEYS);
      const { reportType, ...filtersForApi } = shared;

      const params = {
        page: currentPage,
        size: pageSize,
        status,
        ...filtersForApi,
        hasReport: shared.hasReport ? true : undefined,
      };

      Object.keys(params).forEach(
        (key) =>
          (params[key] === undefined || params[key] === "") && delete params[key]
      );

      const res = await axios.get(
        `${API_BASE_URL}/complaints/paginated-by-status`,
        {
          params,
          withCredentials: true,
        }
      );

      const nextGroups = Array.isArray(res.data.content)
        ? res.data.content.map((group) => ({
            ...group,
            complaints: (group.complaints || []).sort(
              (a, b) => new Date(b.date) - new Date(a.date)
            ),
          }))
        : [];

      setGroups(nextGroups);
      setTotalPages(res.data.totalPages || 1);
      setTotalRecords(res.data.totalElements || 0);

      const beforePageHeader =
        res.headers?.["x-complaints-before-page"] ??
        res.headers?.["X-Complaints-Before-Page"];
      setComplaintsBeforePage(Number.parseInt(beforePageHeader, 10) || 0);

      setProgress(100);
    } catch (err) {
      console.error("Fetch complaints error:", err);
      setGroups([]);
      setTotalPages(1);
      setTotalRecords(0);
      setComplaintsBeforePage(0);
      setProgress(100);
    } finally {
      clearInterval(interval);
      setTimeout(() => setLoading(false), 500);
    }
  }, [API_BASE_URL, currentPage, globalFilters, status]);

  const fetchSingleComplaintAndUpdate = useCallback(
    async (complaintId) => {
      try {
        const res = await axios.get(`${API_BASE_URL}/complaints/by-id`, {
          params: { complaintId },
          withCredentials: true,
        });

        const updatedComplaint = res.data;

        console.log("Updated complaint object:", updatedComplaint);
        console.log(
          "Updated complaint courierStatus:",
          updatedComplaint?.courierStatus
        );

        if (!updatedComplaint) return;

        setGroups((prevGroups) => {
          const belongsInOpenTable =
            updatedComplaint.complaintStatus === "Open";

          return prevGroups
            .map((group) => {
              const hasComplaint = (group.complaints || []).some(
                (c) => c.complaintId === complaintId
              );

              if (!hasComplaint) return group;

              let updatedComplaints;

              if (belongsInOpenTable) {
                updatedComplaints = (group.complaints || []).map((c) =>
                  c.complaintId === complaintId
                    ? { ...c, ...updatedComplaint }
                    : c
                );
              } else {
                updatedComplaints = (group.complaints || []).filter(
                  (c) => c.complaintId !== complaintId
                );
              }

              return {
                ...group,
                complaints: updatedComplaints,
              };
            })
            .filter((group) => (group.complaints || []).length > 0);
        });

        setReportAvailability((prev) => {
          if (!updatedComplaint?.complaintId) return prev;
          return {
            ...prev,
            [updatedComplaint.complaintId]:
              prev[updatedComplaint.complaintId] ?? false,
          };
        });

        fetchDashboardCounts && fetchDashboardCounts();
      } catch (err) {
        console.error("Failed to fetch updated complaint:", err);
      }
    },
    [API_BASE_URL, fetchDashboardCounts]
  );

  useComplaintReportsLive(
    useCallback(
      (wsData) => {
        if (!wsData?.complaintId) return;

        const isVisible = groupsRef.current.some((group) =>
          (group.complaints || []).some(
            (c) => c.complaintId === wsData.complaintId
          )
        );

        if (!isVisible) return;

        fetchSingleComplaintAndUpdate(wsData.complaintId);
      },
      [fetchSingleComplaintAndUpdate]
    )
  );

  useEffect(() => {
    const idsToFetch = allComplaintsFlat
      .map((c) => c.id)
      .filter((id) => id && remarksCounts[id] === undefined);

    if (idsToFetch.length) {
      axios
        .post(`${API_BASE_URL}/complaints/remarks/counts`, idsToFetch, {
          withCredentials: true,
        })
        .then((res) => {
          setRemarksCounts((prev) => ({ ...prev, ...res.data }));
        })
        .catch((err) => {
          console.error("Remarks count fetch error:", err);
        });
    }
  }, [groups, allComplaintsFlat, remarksCounts, API_BASE_URL]);

  useEffect(() => {
    fetchComplaints();
  }, [fetchComplaints, complaintsRefreshKey]);

  useEffect(() => {
    setCurrentPage(0);
  }, [globalFilters]);

  useEffect(() => {
    const visibleComplaintIds = allComplaintsFlat
      .map((c) => c.complaintId)
      .filter((id) => id && reportAvailability[id] === undefined);

    if (visibleComplaintIds.length > 0) {
      axios
        .post(
          `${API_BASE_URL}/hardware-logs/reports/availability`,
          visibleComplaintIds,
          { withCredentials: true }
        )
        .then((response) => {
          setReportAvailability((prev) => ({ ...prev, ...response.data }));
        })
        .catch((err) => {
          console.error("Report availability fetch error:", err);
        });
    }
  }, [groups, allComplaintsFlat, reportAvailability, API_BASE_URL]);

  useEffect(() => {
    axios
      .get(`${API_BASE_URL}/data/banks`, { withCredentials: true })
      .then((res) => setBankList(res.data))
      .catch(() => setBankList([]));

    axios
      .get(`${API_BASE_URL}/data/cities`, { withCredentials: true })
      .then((res) => setCityList(res.data))
      .catch(() => setCityList([]));

    axios
      .get(`${API_BASE_URL}/data/statuses`, { withCredentials: true })
      .then((res) => setStatusList(res.data))
      .catch(() => setStatusList([]));

    axios
      .get(`${API_BASE_URL}/data/visitors`, { withCredentials: true })
      .then((res) => setEngineers(res.data))
      .catch(() => setEngineers([]));
  }, [API_BASE_URL]);

  const handleClearFilters = () => {
    setFilters(defaultFilters);
  };

  const EXPORT_PAGE_SIZE = 1000;
  const EXPORT_BATCH_SIZE = 3;

  const fetchAllComplaintsForExport = async () => {
    const shared = pick(globalFilters, OPEN_ALLOWED_KEYS);
    const { reportType, ...filtersForApi } = shared;

    const firstRes = await axios.get(
      `${API_BASE_URL}/complaints/paginated-by-status`,
      {
        params: { ...filtersForApi, status, page: 0, size: EXPORT_PAGE_SIZE },
        withCredentials: true,
      }
    );

    let allFetched = Array.isArray(firstRes.data?.content)
      ? firstRes.data.content.flatMap((g) => g.complaints || [])
      : [];

    const totalPages = firstRes.data?.totalPages || 1;
    if (totalPages <= 1) return allFetched;

    const pages = Array.from({ length: totalPages - 1 }, (_, i) => i + 1);

    for (let i = 0; i < pages.length; i += EXPORT_BATCH_SIZE) {
      const slice = pages.slice(i, i + EXPORT_BATCH_SIZE);

      const responses = await Promise.all(
        slice.map((p) =>
          axios.get(`${API_BASE_URL}/complaints/paginated-by-status`, {
            params: {
              ...filtersForApi,
              status,
              page: p,
              size: EXPORT_PAGE_SIZE,
            },
            withCredentials: true,
          })
        )
      );

      responses.forEach((res) => {
        if (Array.isArray(res.data?.content)) {
          allFetched = allFetched.concat(
            res.data.content.flatMap((g) => g.complaints || [])
          );
        }
      });
    }

    return allFetched;
  };

  const handleGenerateReport = async (passedFilters) => {
    const filtersToUse = passedFilters || globalFilters;

    if (!filtersToUse.reportType) {
      alert("Please select a report type before generating.");
      return;
    }

    if (filtersToUse.reportType === "daySummaryMulti") {
      const [summaryResp, hardwareResp, engineerResp] = await Promise.all([
        fetch(`${API_BASE_URL}/complaints/complaints-summary`, {
          credentials: "include",
        }),
        fetch(`${API_BASE_URL}/hardware-logs/hardware-dispatch-detail`, {
          credentials: "include",
        }),
        fetch(`${API_BASE_URL}/complaints/city-wise-summary`, {
          credentials: "include",
        }),
      ]);

      if (!summaryResp.ok || !hardwareResp.ok || !engineerResp.ok) {
        alert("Failed to fetch one or more report sections!");
        return;
      }

      const summaryData = await summaryResp.json();
      const hardwareData = await hardwareResp.json();
      const engineerData = await engineerResp.json();
      await generateDaySummaryReport(summaryData, hardwareData, engineerData);
      return;
    }

    try {
      setLoading(true);
      const allComplaints = await fetchAllComplaintsForExport();

      const filteredComplaints = allComplaints.filter(
        (c) => c.complaintStatus !== "Wait For Approval"
      );

      await generateExcelReport(
        filteredComplaints,
        status,
        API_BASE_URL,
        uniqueCities,
        filtersToUse.reportType
      );
    } catch (err) {
      console.error("Export error:", err);
      alert("Failed to fetch all data for export.");
    } finally {
      setLoading(false);
    }
  };

  const handleRowClick = (complaintId) => {
    setSelectedRow((prevSelectedRow) =>
      prevSelectedRow === complaintId ? null : complaintId
    );
  };

  const isSpecialCity = (city) => {
    if (!city) return false;
    const normalized = city.trim().toLowerCase();
    return (
      normalized === "lahore" ||
      normalized === "islamabad" ||
      normalized === "rawalpindi"
    );
  };

  const handleViewHistory = (complaintId) => {
    setSelectedComplaintId(complaintId);
  };

  const handleBackToTable = () => {
    setSelectedComplaintId(null);
  };

  const shouldHighlightRow = (complaint) => {
    const complaintDate = new Date(complaint.date);
    const currentDate = new Date();
    const diffTime = Math.abs(currentDate - complaintDate);
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays > 3;
  };

  const openReportsForComplaint = (complaintId) => {
    setSelectedComplaintIdForReports(complaintId);
    setIsReportModalOpen(true);
  };

  const closeReportModal = () => {
    setSelectedComplaintIdForReports(null);
    setIsReportModalOpen(false);
  };

  const togglePriority = async (complaint) => {
    try {
      const newPriority = !complaint.priority;
      await axios.put(
        `${API_BASE_URL}/complaints/${complaint.id}`,
        { isPriority: newPriority },
        { withCredentials: true }
      );

      // update only local state for this complaint
      setGroups((prevGroups) =>
        prevGroups.map((group) => ({
          ...group,
          complaints: (group.complaints || []).map((c) =>
            c.id === complaint.id ? { ...c, priority: newPriority } : c
          ),
        }))
      );
    } catch (error) {
      console.error("Toggle priority error:", error);
    }
  };

  const toggleInPool = async (complaint) => {
    try {
      const newMarkedInPool = !complaint.markedInPool;
      await axios.patch(
        `${API_BASE_URL}/complaints/${complaint.id}/in-pool`,
        { markedInPool: newMarkedInPool },
        { withCredentials: true }
      );

      // update only local state for this complaint
      setGroups((prevGroups) =>
        prevGroups.map((group) => ({
          ...group,
          complaints: (group.complaints || []).map((c) =>
            c.id === complaint.id
              ? { ...c, markedInPool: newMarkedInPool }
              : c
          ),
        }))
      );
    } catch (error) {
      console.error("Toggle in-pool error:", error);
    }
  };

  let serialCounter = complaintsBeforePage;

  return (
    <div>
      <ComplaintFilters
        filters={globalFilters}
        onFiltersChange={setFilters}
        banks={bankList}
        cities={cityList}
        statuses={statusList}
        engineers={engineers}
        uniqueSubStatuses={uniqueSubStatuses}
        reportTypes={reportTypes}
        totalRecords={totalRecords}
        onClear={handleClearFilters}
        onGenerateReport={handleGenerateReport}
        dateFieldConfig={{
          field: "date",
          label: "Complaint Date",
          placeholder: "yyyy-mm-dd",
        }}
      />

      {selectedComplaintId ? (
        <div>
          <button onClick={handleBackToTable} className="back-button">
            Back to Table
          </button>
          <ComplaintReport complaintId={selectedComplaintId} />
        </div>
      ) : (
        <>
          <table className="complaint-table">
            <thead>
              <tr>
                <th>S.No</th>
                <th>Complaint Status</th>
                <th>Courier Status</th>
                <th>Actions</th>
                <th>In Pool</th>
                <th>Remarks</th>
                <th>Report</th>
                <th>Date</th>
                <th>Bank Name</th>
                <th>Branch Code</th>
                <th>Branch Name</th>
                <th>City</th>
                <th>Reference Number</th>
                <th>Complaint Type</th>
                <th>Details</th>
                <th>Hardware</th>
                <th>Visitor Name</th>
                <th>Repeat Complaint</th>
                <th>Aging Days</th>
                <th>Visit Schedule Date</th>
                <th>Complaint ID</th>
                <th>History</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <Loader progress={progress} />
              ) : groups.length > 0 ? (
                groups.map((group, groupIdx) => (
                  <React.Fragment
                    key={group.bankName + group.branchCode + groupIdx}
                  >
                    <tr className="group-header-row">
                      <td
                        className="group-header-row-data"
                        colSpan="21"
                        style={{
                          fontSize: "16px",
                          fontWeight: "bold",
                          backgroundColor: "#d6eaf8",
                          padding: "10px",
                          lineHeight: "1.6",
                        }}
                      >
                        <span>Bank Name: {group.bankName}</span>
                        {" | "}
                        <span>Branch Code: {group.branchCode}</span>
                        {" | "}
                        <span>Branch Name: {group.branchName}</span>
                      </td>
                    </tr>

                    {(group.complaints || []).map((complaint) => {
                      serialCounter++;
                      return (
                        <tr
                          key={complaint.id}
                          onClick={() => handleRowClick(complaint.id)}
                          className={`
                            ${selectedRow === complaint.id ? "selected-row" : ""}
                            ${shouldHighlightRow(complaint) ? "highlight-row" : ""}
                            ${complaint.priority ? "priority-row" : ""}
                          `}
                        >
                          <td>{serialCounter}</td>

                          <td
                            className={getStatusClass(
                              complaint.complaintStatus
                            )}
                          >
                            <div
                              className="status-hover-container"
                              onMouseEnter={() =>
                                setHoveredComplaintId(complaint.id)
                              }
                              onMouseLeave={() => setHoveredComplaintId(null)}
                            >
                              {complaint.complaintStatus || "Open"}
                              {hoveredComplaintId === complaint.id &&
                                complaint.complaintStatus ===
                                  "Visit Schedule" && (
                                  <div className="visit-schedule-tooltip">
                                    <p>
                                      Schedule Date:{" "}
                                      {complaint.scheduleDate || "N/A"}
                                    </p>
                                    <p>
                                      Engineer: {complaint.visitorName || "N/A"}
                                    </p>
                                    <p>Bank: {complaint.bankName || "N/A"}</p>
                                    <p>
                                      Branch Code:{" "}
                                      {complaint.branchCode || "N/A"}
                                    </p>
                                  </div>
                                )}
                            </div>
                          </td>

                          <td
                            className={getCourierStatusClass(
                              complaint.courierStatus
                            )}
                          >
                            {complaint.courierStatus || "N/A"}
                          </td>

                          <td>
                            <button
                              className="update-button"
                              onClick={(e) => {
                                e.stopPropagation();
                                handleOpenModal(complaint);
                              }}
                            >
                              Update
                            </button>

                            <button
                              className={`priority-button ${
                                complaint.priority ? "priority-active" : ""
                              }`}
                              onClick={(e) => {
                                e.stopPropagation();
                                togglePriority(complaint);
                              }}
                              title={
                                complaint.priority
                                  ? "Unmark Priority"
                                  : "Mark as Priority"
                              }
                              aria-label={
                                complaint.priority
                                  ? "Unmark Priority"
                                  : "Mark as Priority"
                              }
                            >
                              {complaint.priority
                                ? "High Priority"
                                : "Mark as Priority"}
                            </button>
                          </td>

                          {isSpecialCity(complaint.city) ? (
                            <td>
                              <button
                                className={`pool-button ${
                                  complaint.markedInPool ? "active" : ""
                                }`}
                                onClick={async (e) => {
                                  e.stopPropagation();
                                  await toggleInPool(complaint);
                                }}
                                title={
                                  complaint.markedInPool
                                    ? "Remove from Pool"
                                    : "Mark as In Pool"
                                }
                              >
                                {complaint.markedInPool
                                  ? "Remove from Pool"
                                  : "Mark In Pool"}
                                <span
                                  className="city-badge"
                                  title={`Special: ${complaint.city}`}
                                >
                                  🏙️
                                </span>
                              </button>
                            </td>
                          ) : (
                            <td />
                          )}

                          <td>
                            <button
                              className={`remark-button ${
                                remarksCounts[complaint.id] === undefined
                                  ? "remark-button-loading"
                                  : remarksCounts[complaint.id] > 0
                                  ? "remark-button-green"
                                  : "remark-button-red"
                              }`}
                              onClick={(e) => {
                                e.stopPropagation();
                                openRemarksModal(complaint);
                              }}
                              disabled={
                                remarksCounts[complaint.id] === undefined
                              }
                            >
                              {remarksCounts[complaint.id] === undefined
                                ? "Loading..."
                                : "Remarks"}
                              <span
                                className={`remarks-count ${
                                  remarksCounts[complaint.id] > 0
                                    ? "remarks-count-green"
                                    : "remarks-count-red"
                                }`}
                              >
                                {remarksCounts[complaint.id] || 0}
                              </span>
                            </button>
                          </td>

                          <td>
                            {reportAvailability[complaint.complaintId] ===
                            undefined ? (
                              <button
                                className="view-report-button loading-btn"
                                disabled
                              >
                                Checking...
                              </button>
                            ) : reportAvailability[complaint.complaintId] ? (
                              <button
                                className="view-report-button"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  openReportsForComplaint(
                                    complaint.complaintId
                                  );
                                }}
                              >
                                View Reports
                              </button>
                            ) : (
                              <button
                                className="view-report-button grey-button"
                                disabled
                              >
                                No Reports
                              </button>
                            )}
                          </td>

                          <td>{complaint.date}</td>
                          <td>{complaint.bankName}</td>
                          <td>{complaint.branchCode}</td>
                          <td>{complaint.branchName}</td>
                          <td>{complaint.city}</td>
                          <td>{complaint.referenceNumber || "N/A"}</td>
                          <td>{complaint.complaintType || "N/A"}</td>
                          <td
                            className="truncated-cell"
                            title={complaint.details || "No details provided"}
                          >
                            {complaint.details || "No details provided"}
                          </td>
                          <td>{complaint.equipmentDescription}</td>
                          <td>{complaint.visitorName}</td>
                          <td>{complaint.repeatComplaint ? "Yes" : "No"}</td>
                          <td>{calculateAgingDays(complaint.date)}</td>
                          <td>{complaint.scheduleDate || "N/A"}</td>
                          <td>{complaint.complaintId}</td>

                          <td>
                            <button
                              className="view-history-button"
                              onClick={(e) => {
                                e.stopPropagation();
                                handleViewHistory(complaint.complaintId);
                              }}
                            >
                              View History
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </React.Fragment>
                ))
              ) : (
                <tr>
                  <td colSpan="21" style={{ textAlign: "center" }}>
                    No open complaints to display
                  </td>
                </tr>
              )}
            </tbody>
          </table>

          {totalPages > 1 && (
            <div className="pagination-container">
              <button
                className="page-button"
                onClick={() => setCurrentPage(currentPage - 1)}
                disabled={currentPage === 0}
              >
                &lt; Previous
              </button>

              <span style={{ margin: "0 1em" }}>
                Page {currentPage + 1} of {totalPages}
              </span>

              <button
                className="page-button"
                onClick={() => setCurrentPage(currentPage + 1)}
                disabled={currentPage + 1 >= totalPages}
              >
                Next &gt;
              </button>
            </div>
          )}
        </>
      )}

      <ReportModal
        isOpen={isReportModalOpen}
        complaintId={selectedComplaintIdForReports}
        handleClose={closeReportModal}
      />
    </div>
  );
};

export default OpenComplaintsTable;