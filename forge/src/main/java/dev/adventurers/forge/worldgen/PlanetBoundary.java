package dev.adventurers.forge.worldgen;

import dev.adventurers.core.world.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Same-dimension chart crossing. This does not implement rendering across dimensions or invert gravity. */
public final class PlanetBoundary {
    private PlanetBoundary() {}
    public static void tick(ServerLevel level,TerrainSettings settings) {
        var roots=new ArrayList<Entity>();
        for(var entity:level.getAllEntities())if(!entity.isRemoved()&&!entity.isPassenger()&&outside(entity,settings))roots.add(entity);
        for(var root:roots)wrap(root,level,settings);
    }
    private static boolean outside(Entity entity,TerrainSettings settings) {
        return entity.getX() < -settings.circumference()/2.0 || entity.getX() >= settings.circumference()/2.0
                || entity.getZ() < -settings.poleDistance()/2.0 || entity.getZ() >= settings.poleDistance()/2.0;
    }
    public static boolean wrap(Entity root,ServerLevel level,TerrainSettings settings) {
        if(root.isRemoved()||root.isPassenger()||root.level()!=level||!outside(root,settings))return false;
        var point=PlanetCoordinates.normalize(root.getX(),root.getZ(),settings);
        double x=point.x(),z=Math.min(point.z(),settings.poleDistance()/2.0-.001),y=root.getY();
        level.getChunk(BlockPos.containing(x,y,z));
        var box=root.getBoundingBox();
        for(var passenger:root.getSelfAndPassengers().filter(entity->entity!=root).toList())box=box.minmax(passenger.getBoundingBox());
        if(!level.noCollision(root,box.move(x-root.getX(),0,z-root.getZ()))) {
            // Player construction can differ on the opposite edge; avoid embedding the entire riding group.
            y=Math.max(y,level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(int)Math.floor(x),(int)Math.floor(z))+1);
        }
        record Passenger(Entity entity,float yaw,float pitch,Vec3 velocity) {}
        var passengers=root.getSelfAndPassengers().filter(entity->entity!=root).map(p->new Passenger(p,p.getYRot(),p.getXRot(),p.getDeltaMovement())).toList();
        var velocity=root.getDeltaMovement();
        root.teleport(new TeleportTransition(level,new Vec3(x,y,z),point.reflected()?new Vec3(velocity.x,velocity.y,-velocity.z):velocity,
                point.reflected()?180-root.getYRot():root.getYRot(),root.getXRot(),TeleportTransition.DO_NOTHING));
        if(point.reflected())for(var p:passengers) {
            var v=p.velocity();
            p.entity().teleport(new TeleportTransition(level,p.entity().position(),new Vec3(v.x,v.y,-v.z),180-p.yaw(),p.pitch(),TeleportTransition.DO_NOTHING).transitionAsPassenger());
        }
        return true;
    }
}
