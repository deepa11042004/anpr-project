package com.anpr.config;

import com.anpr.service.ScheduledVehicleCsvImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledVehicleBootstrapRunner implements CommandLineRunner {

    private final ScheduledVehicleCsvImportService scheduledVehicleCsvImportService;

    @Value("${anpr.erp.bootstrap-enabled:true}")
    private boolean bootstrapEnabled;

    @Value("${anpr.erp.reload-on-startup:false}")
    private boolean reloadOnStartup;

    @Override
    public void run(String... args) {
        if (!bootstrapEnabled) {
            log.info("ERP CSV bootstrap disabled by configuration.");
            return;
        }

        int imported = scheduledVehicleCsvImportService.importFromCsv(reloadOnStartup);
        if (imported > 0) {
            log.info("ERP bootstrap completed. Imported {} records into scheduled_vehicles.", imported);
        }
    }
}
