import React, { useState, useEffect } from "react";
import "./ComplaintFilters.css";

const defaultFilterState = {
  status: "Open",
  bankName: "",
  branchCode: "",
  branchName: "",
  engineerName: "",
  city: "",
  complaintStatus: "",
  subStatus: "",
  date: "",
  dateFrom: "",
  dateTo: "",
  priority: "",
  inPool: "",
  hasReport: false,
  reportType: "",
};

const ComplaintFilters = ({
  filters,
  onFiltersChange,
  banks,
  engineers,
  cities,
  statuses,
  reportTypes,
  totalRecords,
  onClear,
  onGenerateReport,
  dateFieldConfig,
}) => {
  // Local state for filter values
  const [localFilters, setLocalFilters] = useState(
    filters || defaultFilterState
  );

  // Sync local state with parent filters (e.g., when "Clear" is pressed)
  useEffect(() => {
    setLocalFilters(filters || defaultFilterState);
  }, [filters]);

  // Local handler for inputs
  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setLocalFilters((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  // Trigger parent search
  const handleSearch = (e) => {
    e && e.preventDefault();
    onFiltersChange(localFilters);
  };

  // Trigger on Enter
  const handleKeyDown = (e) => {
    if (e.key === "Enter") {
      handleSearch(e);
    }
  };

  // Clear local and parent filters
  const handleClear = () => {
    setLocalFilters(defaultFilterState);
    onClear();
  };

  return (
    <form
      className="complaint-adv-filters-bar"
      onSubmit={handleSearch}
      onKeyDown={handleKeyDown}
      autoComplete="off"
    >
      <div className="complaint-adv-filter-row">
        {/* Date range */}
        {dateFieldConfig && (
          <div className="complaint-adv-filter-field complaint-adv-filter-date-range">
            <label>{dateFieldConfig.label || "Date"} Range:</label>
            <div className="complaint-adv-date-range-fields-vertical">
              <input
                type="date"
                name={dateFieldConfig.field + "From"}
                value={localFilters[dateFieldConfig.field + "From"] || ""}
                onChange={handleChange}
                placeholder="From"
                style={{ marginBottom: 6 }}
              />
              <span
                className="complaint-adv-date-to-label"
                style={{ marginBottom: 6 }}
              >
                to
              </span>
              <input
                type="date"
                name={dateFieldConfig.field + "To"}
                value={localFilters[dateFieldConfig.field + "To"] || ""}
                onChange={handleChange}
                placeholder="To"
              />
            </div>
          </div>
        )}

        {/* Exact date */}
        {dateFieldConfig && (
          <div className="complaint-adv-filter-field">
            <label>{dateFieldConfig.label || "Date"} (Exact):</label>
            <input
              type="date"
              name={dateFieldConfig.field}
              value={localFilters[dateFieldConfig.field] || ""}
              onChange={handleChange}
              placeholder="yyyy-mm-dd"
            />
          </div>
        )}

        {/* Bank Name */}
        <div className="complaint-adv-filter-field">
          <label>Bank Name:</label>
          <input
            list="bankList"
            name="bankName"
            value={localFilters.bankName}
            onChange={handleChange}
            placeholder="All Banks"
            autoComplete="off"
          />
          <datalist id="bankList">
            {banks &&
              banks.map((bank) => <option key={bank.id} value={bank.name} />)}
          </datalist>
        </div>

        {/* Engineer Name */}
        <div className="complaint-adv-filter-field">
          <label>Engineer Name:</label>
          <input
            list="engineerList"
            name="engineerName"
            value={localFilters.engineerName}
            onChange={handleChange}
            placeholder="All Engineers"
            autoComplete="off"
          />
          <datalist id="engineerList">
            {engineers &&
              engineers.map((eng) => (
                <option
                  key={eng.id}
                  value={eng.name || eng.visitorName || eng.username}
                />
              ))}
          </datalist>
        </div>

        {/* City */}
        <div className="complaint-adv-filter-field">
          <label>City:</label>
          <input
            list="cityList"
            name="city"
            value={localFilters.city}
            onChange={handleChange}
            placeholder="All Cities"
            autoComplete="off"
          />
          <datalist id="cityList">
            {cities &&
              cities.map((city) => <option key={city.id} value={city.name} />)}
          </datalist>
        </div>
      </div>

      <div className="complaint-adv-filter-row">
        <div className="complaint-adv-filter-field">
          <label>Branch Code:</label>
          <input
            type="text"
            name="branchCode"
            value={localFilters.branchCode}
            onChange={handleChange}
            placeholder="Search Branch Code"
            autoComplete="off"
          />
        </div>
        <div className="complaint-adv-filter-field">
          <label>Branch Name:</label>
          <input
            type="text"
            name="branchName"
            value={localFilters.branchName}
            onChange={handleChange}
            placeholder="Search Branch Name"
            autoComplete="off"
          />
        </div>
        <div className="complaint-adv-filter-field">
          <label>Status:</label>
          <select
            name="complaintStatus"
            value={localFilters.complaintStatus}
            onChange={handleChange}
          >
            <option value="">All Statuses</option>
            <option value="FOC_APPROVED">FOC &amp; Approved</option>
            {statuses &&
              statuses.map((status) => (
                <option key={status.id} value={status.statusValue}>
                  {status.statusValue}
                </option>
              ))}
          </select>
        </div>

        <div className="complaint-adv-filter-checkbox">
          <input
            type="checkbox"
            name="hasReport"
            checked={localFilters.hasReport || false}
            onChange={handleChange}
            id="hasReportCheckbox"
          />
          <label
            htmlFor="hasReportCheckbox"
            className="complaint-adv-checkbox-label"
          >
            Has Report
          </label>
        </div>
        <div className="complaint-adv-filter-field">
          <label style={{ visibility: "hidden" }}>Spacer</label>
          <span style={{ color: "#333", fontSize: "15px", fontWeight: 500 }}>
            Found Records: {totalRecords}
          </span>
        </div>
        <div className="complaint-adv-filter-buttons">
          <button
            className="complaint-adv-clear-btn"
            onClick={handleClear}
            type="button"
          >
            Clear Filters
          </button>
        </div>
      </div>

      <div className="complaint-adv-filter-row">
        <div className="complaint-adv-filter-field" style={{ minWidth: 260 }}>
          <label>Select Report Type:</label>
          <select
            name="reportType"
            value={localFilters.reportType || ""}
            onChange={handleChange}
          >
            <option value="">-- Choose a Report --</option>
            {reportTypes.map((rt) => (
              <option value={rt.value} key={rt.value}>
                {rt.label}
              </option>
            ))}
          </select>
        </div>
        <div className="complaint-adv-filter-buttons">
          <button
            className="complaint-adv-generate-btn"
            onClick={() => {
              onFiltersChange(localFilters); // Still optional, for parent sync
              onGenerateReport(localFilters); // << PASS FILTERS HERE!
            }}
            type="button"
          >
            Generate Report
          </button>
        </div>
        {/* SEARCH BUTTON - new */}
        <div className="complaint-adv-filter-buttons">
          <button
            className="complaint-adv-search-btn"
            type="submit"
            style={{ marginLeft: 8 }}
          >
            Search
          </button>
        </div>
      </div>
    </form>
  );
};

export default ComplaintFilters;
