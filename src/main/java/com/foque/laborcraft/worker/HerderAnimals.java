package com.foque.laborcraft.worker;

import com.foque.laborcraft.area.EnclosureDescriptor;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Herdable species and their breeding food. Vanilla tags are used so modded seeds/wheat also work.
 * Pigs are intentionally absent for now: their product requires killing.
 *
 * <p>The brain takes one {@link #snapshot} per check and reuses it for tool choice, population and
 * breeding, so a pen with many animals is not scanned several times per tick.</p>
 */
public final class HerderAnimals {
	public enum Species {
		CHICKEN(ItemTags.CHICKEN_FOOD),
		SHEEP(ItemTags.SHEEP_FOOD),
		COW(ItemTags.COW_FOOD);

		private final TagKey<Item> food;

		Species(TagKey<Item> food) {
			this.food = food;
		}

		public boolean isFood(ItemStack stack) {
			return !stack.isEmpty() && stack.is(food);
		}
	}

	private static final int VERTICAL_SCAN = 12;

	private HerderAnimals() {
	}

	public static Species speciesOf(Entity entity) {
		if (entity instanceof Chicken) {
			return Species.CHICKEN;
		}
		if (entity instanceof Sheep) {
			return Species.SHEEP;
		}
		if (entity instanceof Cow) {
			return Species.COW;
		}
		return null;
	}

	public static boolean isHerdable(Entity entity) {
		return speciesOf(entity) != null;
	}

	/** One entity query per pen; callers reuse the result for counting, tools and breeding. */
	public static Map<Species, List<Animal>> snapshot(ServerLevel level, EnclosureDescriptor pen) {
		Map<Species, List<Animal>> herd = new EnumMap<>(Species.class);
		for (Species species : Species.values()) {
			herd.put(species, new ArrayList<>());
		}

		int y = pen.seed().getY();
		AABB box = new AABB(
				pen.min().getX(),
				y - VERTICAL_SCAN,
				pen.min().getZ(),
				pen.max().getX() + 1.0D,
				y + VERTICAL_SCAN,
				pen.max().getZ() + 1.0D
		);
		for (Animal animal : level.getEntitiesOfClass(Animal.class, box)) {
			if (!animal.isAlive() || !pen.contains(animal.blockPosition())) {
				continue;
			}
			Species species = speciesOf(animal);
			if (species != null) {
				herd.get(species).add(animal);
			}
		}
		return herd;
	}

	public static int count(Map<Species, List<Animal>> herd, Species species) {
		return herd.getOrDefault(species, List.of()).size();
	}

	public static int adultCount(Map<Species, List<Animal>> herd, Species species) {
		int count = 0;
		for (Animal animal : herd.getOrDefault(species, List.of())) {
			if (!animal.isBaby()) {
				count++;
			}
		}
		return count;
	}

	/** Adults of one species that can still fall in love. */
	public static List<Animal> breedableAdults(Map<Species, List<Animal>> herd, Species species) {
		List<Animal> adults = new ArrayList<>();
		for (Animal animal : herd.getOrDefault(species, List.of())) {
			if (animal.isBaby() || animal.isInLove() || !animal.canFallInLove()) {
				continue;
			}
			adults.add(animal);
		}
		return adults;
	}

	public static boolean has(Map<Species, List<Animal>> herd, Species species) {
		return !herd.getOrDefault(species, List.of()).isEmpty();
	}
}
