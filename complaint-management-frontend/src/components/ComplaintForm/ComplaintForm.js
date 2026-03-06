import React, { useState, useEffect, useMemo } from "react";
import axios from "axios";
import CreatableSelect from "react-select/creatable";
import "react-datepicker/dist/react-datepicker.css"; // Still imported in case other parts rely on it
import "./complaintForm.css";

const ComplaintForm = ({ onSubmit }) => {
  const [formData, setFormData] = useState({
    date: new Date().toISOString().split("T")[0],
    bankName: "",
    branchCode: "",
    branchName: "",
    city: "",
    referenceNumber: "",
    complaintType: "",
    visitorName: "",
    visitorId: null,
    complaintStatus: "Open", // Default now "Open"
    details: "",
  });

  const [banks, setBanks] = useState([]);
  const [cities, setCities] = useState([]);
  const [visitors, setVisitors] = useState([]);
  const [branches, setBranches] = useState([]);
  const [filteredVisitors, setFilteredVisitors] = useState([]);
  const [complaintTypes, setComplaintTypes] = useState([]);
  const [validationErrors, setValidationErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [branchesLoading, setBranchesLoading] = useState(false);

  const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;

  useEffect(() => {
    const fetchInitialData = async () => {
      try {
        const [banksRes, visitorsRes, typesRes, citiesRes] = await Promise.all([
          axios.get(`${API_BASE_URL}/data/banks`, { withCredentials: true }),
          axios.get(`${API_BASE_URL}/data/visitors`, { withCredentials: true }),
          axios.get(`${API_BASE_URL}/data/complaint-types`, { withCredentials: true }),
          axios.get(`${API_BASE_URL}/data/cities`, { withCredentials: true }),
        ]);

        setBanks(Array.isArray(banksRes.data) ? banksRes.data : []);
        setVisitors(Array.isArray(visitorsRes.data) ? visitorsRes.data : []);
        setFilteredVisitors(Array.isArray(visitorsRes.data) ? visitorsRes.data : []);
        setComplaintTypes(Array.isArray(typesRes.data) ? typesRes.data : []);
        setCities(Array.isArray(citiesRes.data) ? citiesRes.data : []);
      } catch (error) {
        console.error("Error fetching initial form data:", error);
      }
    };

    fetchInitialData();
  }, [API_BASE_URL]);

  /**
   * Fetch branches for a specific bank and sort them by numeric code, then by name.
   */
  const fetchBranches = async (bankName) => {
    if (!bankName) {
      setBranches([]);
      return;
    }

    setBranchesLoading(true);
    try {
      const response = await axios.get(
        `${API_BASE_URL}/data/branches?bank=${encodeURIComponent(bankName)}`,
        { withCredentials: true }
      );
      const fetchedBranches = Array.isArray(response.data) ? response.data : [];

      // Sort by numeric branchCode, then by branchName
      const sortedBranches = fetchedBranches.sort((a, b) => {
        const codeA = parseInt(a.branchCode, 10);
        const codeB = parseInt(b.branchCode, 10);
        if (codeA !== codeB) return codeA - codeB;
        return a.branchName.localeCompare(b.branchName);
      });

      setBranches(sortedBranches);
    } catch (error) {
      console.error("Error fetching branches:", error);
      setBranches([]);
    } finally {
      setBranchesLoading(false);
    }
  };

  /**
   * Validate individual fields.
   */
  const validateField = (name, value) => {
    const errors = { ...validationErrors };

    if (name === "bankName" && !value) {
      errors[name] = "Bank name is required.";
    } else if (name === "branchCode") {
      if (!value) {
        errors[name] = "Branch code is required.";
      } else if (!/^\d{4}$/.test(value)) {
        errors[name] = "Branch code must be exactly 4 digits.";
      } else {
        delete errors[name];
      }
    } else if (name === "city" && !value) {
      errors[name] = "City is required.";
    } else if (name === "referenceNumber" && value.length > 20) {
      errors[name] = "Reference number cannot exceed 20 characters.";
    } else {
      delete errors[name];
    }

    setValidationErrors(errors);
  };

  /**
   * Handle changes in the Bank (CreatableSelect).
   */
  const handleBankChange = (selectedOption) => {
    const selectedBank = selectedOption ? selectedOption.value : "";

    setFormData((prev) => ({
      ...prev,
      bankName: selectedBank,
      branchCode: "",
      branchName: "",
      city: "",
    }));

    validateField("bankName", selectedBank);
    fetchBranches(selectedBank);
  };

  /**
   * Handle user-creation of a new bank.
   */
  const handleBankCreate = (inputValue) => {
    setBanks((prevBanks) => [...prevBanks, { name: inputValue }]);

    setFormData((prev) => ({
      ...prev,
      bankName: inputValue,
      branchCode: "",
      branchName: "",
      city: "",
    }));

    validateField("bankName", inputValue);
    fetchBranches(inputValue);
  };

  /**
   * Handle Branch Code selection (CreatableSelect).
   */
  const handleBranchSelect = (selectedOption) => {
    const branchCode = selectedOption ? selectedOption.value : "";

    setFormData((prev) => {
      const selectedBranch = branches.find(
        (branch) => branch.branchCode === branchCode && branch.bank === prev.bankName
      );

      // Ensure 4-digit code via padStart
      const formattedBranchCode = branchCode.padStart(4, "0");

      return {
        ...prev,
        branchCode: formattedBranchCode,
        branchName: selectedBranch ? selectedBranch.branchName : prev.branchName,
      };
    });

    // Validate 4-digit code
    validateField("branchCode", branchCode.padStart(4, "0"));
  };

  /**
   * Handle user creation of a new Branch Code in the CreatableSelect.
   */
  const handleBranchCreate = (inputValue) => {
    // Ensure 4-digit code via padStart
    const formattedValue = inputValue.padStart(4, "0");

    setBranches((prevBranches) => [
      ...prevBranches,
      {
        bank: formData.bankName,
        branchCode: formattedValue,
        branchName: formattedValue, // Default branchName same as code
      },
    ]);

    setFormData((prev) => ({
      ...prev,
      branchCode: formattedValue,
      branchName: formattedValue,
    }));

    validateField("branchCode", formattedValue);
  };

  /**
   * Handle change for any regular <input>, <select>, etc.
   */
  const handleChange = (e) => {
    const { name, value } = e.target;

    setFormData((prevFormData) => {
      let updatedFormData = { ...prevFormData, [name]: value };

      if (name === "visitorName") {
        const selectedVisitor = visitors.find((visitor) => visitor.name === value);
        updatedFormData.visitorId = selectedVisitor?.id || null;
      }

      if (name === "city") {
        const filtered = visitors.filter(
          (visitor) =>
            visitor.city &&
            visitor.city.trim().toLowerCase() === value.trim().toLowerCase()
        );
        setFilteredVisitors(filtered.length > 0 ? filtered : visitors);
      }

      return updatedFormData;
    });

    validateField(name, value);
  };

  /**
   * Submission
   */
  const handleSubmit = async (e) => {
    e.preventDefault();
  
    // Validate fields
    Object.keys(formData).forEach((field) => validateField(field, formData[field]));
  
    // If we already have validation errors, stop
    if (Object.keys(validationErrors).length > 0) {
      alert("Please fix the validation errors before submitting.");
      return;
    }
  
    // Ensure branchCode is exactly 4 digits
    if (!/^\d{4}$/.test(formData.branchCode)) {
      alert("Branch Code must be exactly 4 digits. Please correct it.");
      return;
    }
  
    setLoading(true);
  
    try {
      // FAST DUPLICATE CHECK!
      const duplicateRes = await axios.get(
        `${API_BASE_URL}/complaints/duplicate`,
        {
          params: { 
            bankName: formData.bankName, 
            branchCode: formData.branchCode 
          },
          withCredentials: true,
        }
      );
  
      if (duplicateRes.data) {
        const userConfirmed = window.confirm(
          "A similar complaint is already logged. Do you still want to proceed?"
        );
        if (!userConfirmed) {
          setLoading(false);
          return;
        }
      }
  
      // Log the complaint
      const response = await axios.post(`${API_BASE_URL}/complaints/log`, formData, {
        withCredentials: true,
      });
  
      if (typeof onSubmit === "function") {
        onSubmit(response.data);
      }
  
      // Reset form
      setFormData({
        date: new Date().toISOString().split("T")[0],
        bankName: "",
        branchCode: "",
        branchName: "",
        city: "",
        referenceNumber: "",
        complaintType: "",
        visitorName: "",
        visitorId: null,
        complaintStatus: "Open",
        details: "",
      });
      setBranches([]);
      setFilteredVisitors(visitors);
    } catch (error) {
      console.error("Error logging complaint:", error);
      alert("An error occurred while submitting the complaint. Please try again.");
    } finally {
      setLoading(false);
    }
  };
  


  /**
   * Memoized options
   */
  const bankOptions = useMemo(
    () =>
      banks.map((bank) => ({
        value: bank.name,
        label: bank.name,
      })),
    [banks]
  );

  const branchOptions = useMemo(
    () =>
      branches
        .filter((b) => b.bank === formData.bankName)
        .map((branch) => ({
          value: branch.branchCode,
          label: `${branch.branchCode} - ${branch.branchName}`,
        })),
    [branches, formData.bankName]
  );

  const cityOptions = cities.map((city) => (
    <option key={city.id} value={city.name}>
      {city.name}
    </option>
  ));

  return (
    <div className="complaint-form">
      <h2>Register a New Complaint</h2>
      <form onSubmit={handleSubmit}>
        {/* Date, Bank Name, Branch Code Row */}
        <div className="form-row">
          <div className="form-section">
            <label>Date</label>
            <input
              type="date"
              name="date"
              value={formData.date}
              readOnly
              disabled
            />
          </div>

          <div className="form-section">
            <label>Bank Name</label>
            <CreatableSelect
              options={bankOptions}
              value={
                formData.bankName
                  ? { value: formData.bankName, label: formData.bankName }
                  : null
              }
              onChange={handleBankChange}
              onCreateOption={handleBankCreate}
              isClearable
              placeholder="Select or Add Bank Name"
            />
            {validationErrors.bankName && (
              <span className="error">{validationErrors.bankName}</span>
            )}
          </div>

          <div className="form-section">
            <label>Branch Code</label>
            <CreatableSelect
              options={branchOptions}
              value={
                formData.branchCode
                  ? {
                      value: formData.branchCode,
                      label: `${formData.branchCode} - ${
                        formData.branchName || "Branch Name"
                      }`,
                    }
                  : null
              }
              onChange={handleBranchSelect}
              onCreateOption={handleBranchCreate}
              isClearable
              isDisabled={!formData.bankName || branchesLoading}
              isLoading={branchesLoading}
              placeholder={
                branchesLoading ? "Loading branches..." : "Select or Add Branch Code"
              }
              maxMenuHeight={200}
            />
            {validationErrors.branchCode && (
              <span className="error">{validationErrors.branchCode}</span>
            )}
          </div>
        </div>

        {/* Branch Name, City, Reference Number Row */}
        <div className="form-row">
          <div className="form-section">
            <label>Branch Name</label>
            <input
              type="text"
              name="branchName"
              value={formData.branchName}
              onChange={handleChange}
              placeholder="Enter Branch Name"
            />
          </div>

          <div className="form-section">
            <label>City</label>
            <select name="city" value={formData.city} onChange={handleChange} required>
              <option value="">Select City</option>
              {cityOptions}
            </select>
            {validationErrors.city && (
              <span className="error">{validationErrors.city}</span>
            )}
          </div>

          <div className="form-section">
            <label>Reference Number</label>
            <input
              type="text"
              name="referenceNumber"
              value={formData.referenceNumber}
              onChange={handleChange}
              maxLength="20"
            />
            {validationErrors.referenceNumber && (
              <span className="error">{validationErrors.referenceNumber}</span>
            )}
          </div>
        </div>

        {/* 
          Schedule Date row REMOVED
          Instead, we'll go directly to the Visitor Name 
        */}

        <div className="form-row">
          <div className="form-section">
            <label>Visitor Name</label>
            <select
              name="visitorName"
              value={formData.visitorName}
              onChange={handleChange}
              // required
            >
              <option value="">Select Visitor</option>
              {filteredVisitors.map((visitor) => (
                <option key={visitor.id} value={visitor.name}>
                  {visitor.name} - {visitor.city || "No City"}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Complaint Type, Details Row */}
        <div className="form-row">
          <div className="form-section">
            <label>Complaint Type</label>
            <select
              name="complaintType"
              value={formData.complaintType}
              onChange={handleChange}
            >
              <option value="">Select Complaint Type</option>
              {complaintTypes.map((type) => (
                <option key={type.id} value={type.name}>
                  {type.name}
                </option>
              ))}
            </select>
          </div>

          <div className="form-section full-width">
            <label>Details</label>
            <textarea
              name="details"
              value={formData.details}
              onChange={handleChange}
              rows="5"
            />
          </div>
        </div>

        {/* Submit Button */}
        <button className="smt-btn" type="submit" disabled={loading}>
          {loading ? "Submitting..." : "Submit Complaint"}
        </button>
      </form>
    </div>
  );
};

export default ComplaintForm;
