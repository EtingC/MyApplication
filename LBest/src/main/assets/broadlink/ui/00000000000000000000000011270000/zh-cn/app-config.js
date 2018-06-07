define(function() {
    var deviceConfig = {};
    deviceConfig.dashboard = {
        attr:[
            {
                key: "OnOff_Power",
                label: "电源开关",
                type: "xz_master_switch",
                dtype: 1,
                position: ['home', 'device'],
                values: ["1", "2"],
                valueLabels: ["工作中", "通电待机"]
            }
        ]
    };
    return deviceConfig;
});
