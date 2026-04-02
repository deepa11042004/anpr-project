-- Seed data for testing ANPR decision scenarios on 2026-03-31
-- Target table: scheduled_vehicles
-- Safe to run multiple times (uses MERGE by queue_no).

MERGE INTO scheduled_vehicles (
    queue_no,
    uplift_type,
    date_created,
    omc,
    omc_name,
    uplift_date,
    ticket_number,
    product_name,
    requested_quantity,
    truck_reg_no,
    trailor_no,
    driver_name,
    status,
    location
) KEY (queue_no) VALUES
(120, 'Own Stock', DATE '2026-03-31', 'P007', 'PUMA ENERGY ZAMBIA PLC', DATE '2026-03-31', 'LU-20310', 'Unleaded Petrol', 40000.00, 'BME1200ZM', 'BME2200ZM', 'FRANCIS M', 'Security', 'Ndola Fuel Terminal'),
(121, 'Own Stock', DATE '2026-03-31', 'R004', 'RUBIS ENERGY ZAMBIA LIMITED', DATE '2026-03-31', 'RB-45011', 'Low Sulphur Gas Oil', 38000.00, 'BME1201ZM', 'BME2201ZM', 'ELIAS K', 'Active', 'Ndola Fuel Terminal'),
(122, 'Own Stock', DATE '2026-03-31', 'L001', 'LAKE PETROLEUM LTD', DATE '2026-03-31', 'LK-77812', 'Unleaded Petrol', 36000.00, 'BME1202ZM', 'BME2202ZM', 'REAGAN B', 'Security', 'Ndola Fuel Terminal'),
(123, 'Own Stock', DATE '2026-03-31', 'A019', 'ASHARAMI ENERGY LIMITED', DATE '2026-03-31', 'AS-99113', 'Low Sulphur Gas Oil', 41000.00, 'BME1203ZM', 'BME2203ZM', 'STEPHEN N', 'Security', 'Ndola Fuel Terminal'),

-- Rejection scenarios for rules testing
(124, 'Own Stock', DATE '2026-03-31', 'M004', 'MOUNT MERU PETROLEUM (Z) LTD', DATE '2026-03-31', 'MM-55214', 'Unleaded Petrol', 35000.00, 'BME1204ZM', 'BME2204ZM', 'CHISENGA K', 'Blocked', 'Ndola Fuel Terminal'),
(125, 'Own Stock', DATE '2026-03-31', 'H002', 'HASS PETROLEUM ZAMBIA LTD', DATE '2026-03-31', 'HS-33215', 'Low Sulphur Gas Oil', 150000.00, 'BME1205ZM', 'BME2205ZM', 'KALOBWE T', 'Security', 'Ndola Fuel Terminal'),
(126, 'Own Stock', DATE '2026-04-01', 'K018', 'KORRIDOR ZAMBIA FUEL LTD', DATE '2026-03-31', 'KZ-44116', 'Low Sulphur Gas Oil', 32000.00, 'BME1206ZM', 'BME2206ZM', 'MATALE F', 'Security', 'Ndola Fuel Terminal'),
(127, 'Own Stock', DATE '2026-03-31', 'E020', 'EQUAL ENERGY ZAMBIA LIMITED', DATE '2026-03-31', 'EQ-22117', 'Unleaded Petrol', 34000.00, 'BME1207ZM', 'BME2207ZM', '', 'Security', 'Ndola Fuel Terminal'),

-- Queue order testing (attempting this plate before 120-123 should be rejected when enforce-queue-order=true)
(130, 'Own Stock', DATE '2026-03-31', 'S053', 'SM IGNITE ENERGIES LIMITED', DATE '2026-03-31', 'SM-11830', 'Low Sulphur Gas Oil', 39000.00, 'BME1210ZM', 'BME2210ZM', 'DICKSON P', 'Security', 'Ndola Fuel Terminal');
