package com.taobao.arthas.core.command.monitor200;

import com.alibaba.bytekit.asm.MethodProcessor;
import com.alibaba.bytekit.asm.location.Location;
import com.alibaba.bytekit.asm.location.LocationMatcher;
import com.alibaba.bytekit.asm.location.LocationType;
import com.alibaba.bytekit.asm.location.filter.LocationFilter;
import com.alibaba.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.taobao.arthas.core.util.look.LookUtils;

import java.util.ArrayList;
import java.util.List;

public class LookLocationMatcher implements LocationMatcher {

    private String locCode = "";

    public LookLocationMatcher(String locCode) {
        if (!LookUtils.validLookLocation(locCode)) {
            throw new IllegalArgumentException("locCode is illegal!");
        }
        this.locCode = locCode;
    }

    @Override
    public List<Location> match(MethodProcessor methodProcessor) {
        List<Location> locations = new ArrayList<Location>();
        LocationFilter locationFilter = methodProcessor.getLocationFilter();

        AbstractInsnNode insnNode = LookUtils.findInsnNode(methodProcessor.getMethodNode(), locCode);
        //一定是往后检查，因为也往后插入的，为什么一定往后插呢？因为即使第一条就是赋值，那插入前面也没意义啊
        boolean filtered = !locationFilter.allow(insnNode, LocationType.USER_DEFINE, true);
        Location location = new LookLocation(insnNode, locCode, true, filtered);
        locations.add(location);
        return locations;
    }

    /**
     * 定义一个LookLocation
     * 为什么？因为其它已经定义的好的Location不太适用
     */
    public static class LookLocation extends Location{

        private String lookLoc;

        public LookLocation(AbstractInsnNode insnNode,String lookLoc, boolean whenComplete, boolean filtered) {
            super(insnNode, whenComplete, filtered);
            this.lookLoc = lookLoc;
        }

        @Override
        public LocationType getLocationType() {
            return LocationType.USER_DEFINE;
        }

        public String getLookLoc() {
            return lookLoc;
        }

        public void setLookLoc(String lookLoc) {
            this.lookLoc = lookLoc;
        }

    }

}
