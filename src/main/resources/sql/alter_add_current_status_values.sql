-- warning_current_status 表新增实测/预报值字段
ALTER TABLE warning_current_status ADD (actual_value DOUBLE);
ALTER TABLE warning_current_status ADD (forecast_value DOUBLE);
ALTER TABLE warning_current_status ADD (forecast_time DATE);
ALTER TABLE warning_current_status ADD (forecast_period INT);
