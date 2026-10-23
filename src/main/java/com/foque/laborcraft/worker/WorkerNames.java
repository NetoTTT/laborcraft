package com.foque.laborcraft.worker;

import java.util.List;
import net.minecraft.util.RandomSource;

/**
 * Nomes proprios para as pessoas da loja. A pessoa contratada e alguem, nao um "cargo padrao".
 */
public final class WorkerNames {
	private static final List<String> FIRST_NAMES = List.of(
			"Bento", "Clara", "Dario", "Elisa", "Fabricio", "Gilda", "Heitor", "Iracema",
			"Jonas", "Lidia", "Mauro", "Neusa", "Otavio", "Paula", "Quirino", "Rosa",
			"Salvio", "Tereza", "Ubirajara", "Vera", "Waldir", "Yara", "Zeca", "Alma",
			"Braulio", "Celina", "Dolores", "Elias", "Fatima", "Gilberto", "Helena", "Ivo"
	);

	private static final List<String> LAST_NAMES = List.of(
			"Almeida", "Barbosa", "Cardoso", "Duarte", "Esteves", "Ferreira", "Gomes",
			"Henriques", "Ibrahim", "Justino", "Lacerda", "Macedo", "Nogueira", "Oliveira",
			"Pereira", "Queiroz", "Ribeiro", "Salgado", "Teixeira", "Uchoa", "Vasconcelos"
	);

	private WorkerNames() {
	}

	public static String random(RandomSource random) {
		String first = FIRST_NAMES.get(random.nextInt(FIRST_NAMES.size()));
		String last = LAST_NAMES.get(random.nextInt(LAST_NAMES.size()));
		return first + " " + last;
	}
}
