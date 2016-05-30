package io.thingcare.web.rest;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import io.thingcare.ThingCareApplication;
import io.thingcare.api.asset.model.AssetDto;
import io.thingcare.modules.asset.Asset;
import io.thingcare.modules.asset.AssetMapper;
import io.thingcare.modules.asset.AssetRepository;
import io.thingcare.modules.asset.AssetResource;
import io.thingcare.modules.asset.AssetService;

/**
 * Test class for the AssetResource REST controller.
 *
 * @see AssetResource
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ThingCareApplication.class)
@WebAppConfiguration
@IntegrationTest
public class AssetResourceIntTest {

	private static final String DEFAULT_NAME = "AAAAA";
	private static final String UPDATED_NAME = "BBBBB";

	@Inject
	private AssetRepository assetRepository;

	@Inject
	private AssetMapper assetMapper;

	@Inject
	private AssetService assetService;

	@Inject
	private MappingJackson2HttpMessageConverter jacksonMessageConverter;

	@Inject
	private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

	private MockMvc restAssetMockMvc;

	private Asset asset;

	@PostConstruct
	public void setup() {
		MockitoAnnotations.initMocks(this);
		AssetResource assetResource = new AssetResource();
		ReflectionTestUtils.setField(assetResource, "assetService", assetService);
		ReflectionTestUtils.setField(assetResource, "assetMapper", assetMapper);
		this.restAssetMockMvc = MockMvcBuilders	.standaloneSetup(assetResource)
												.setCustomArgumentResolvers(pageableArgumentResolver)
												.setMessageConverters(jacksonMessageConverter)
												.build();
	}

	@Before
	public void initTest() {
		assetRepository.deleteAll();
		asset = new Asset();
		asset.setName(DEFAULT_NAME);
	}

	@Test
	public void createAsset() throws Exception {
		int databaseSizeBeforeCreate = assetRepository	.findAll()
														.size();

		// Create the Asset
		AssetDto assetDto = assetMapper.asDto(asset);

		restAssetMockMvc.perform(post("/api/assets").contentType(TestUtil.APPLICATION_JSON_UTF8)
													.content(TestUtil.convertObjectToJsonBytes(assetDto)))
						.andExpect(status().isCreated());

		// Validate the Asset in the database
		List<Asset> assets = assetRepository.findAll();
		assertThat(assets).hasSize(databaseSizeBeforeCreate + 1);
		Asset testAsset = assets.get(assets.size() - 1);
		assertThat(testAsset.getName()).isEqualTo(DEFAULT_NAME);
	}

	@Test
	public void checkNameIsRequired() throws Exception {
		int databaseSizeBeforeTest = assetRepository.findAll()
													.size();
		// set the field null
		asset.setName(null);

		// Create the Asset, which fails.
		AssetDto assetDto = assetMapper.asDto(asset);

		restAssetMockMvc.perform(post("/api/assets").contentType(TestUtil.APPLICATION_JSON_UTF8)
													.content(TestUtil.convertObjectToJsonBytes(assetDto)))
						.andExpect(status().isBadRequest());

		List<Asset> assets = assetRepository.findAll();
		assertThat(assets).hasSize(databaseSizeBeforeTest);
	}

	@Test
	public void getAllAssets() throws Exception {
		// Initialize the database
		assetRepository.save(asset);

		// Get all the assets
		restAssetMockMvc.perform(get("/api/assets?sort=id,desc"))
						.andExpect(status().isOk())
						.andExpect(content().contentType(MediaType.APPLICATION_JSON))
						.andExpect(jsonPath("$.[*].id").value(hasItem(asset.getId())))
						.andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())));
	}

	@Test
	public void getAsset() throws Exception {
		// Initialize the database
		assetRepository.save(asset);

		// Get the asset
		restAssetMockMvc.perform(get("/api/assets/{id}", asset.getId()))
						.andExpect(status().isOk())
						.andExpect(content().contentType(MediaType.APPLICATION_JSON))
						.andExpect(jsonPath("$.id").value(asset.getId()))
						.andExpect(jsonPath("$.name").value(DEFAULT_NAME.toString()));
	}

	@Test
	public void getNonExistingAsset() throws Exception {
		// Get the asset
		restAssetMockMvc.perform(get("/api/assets/{id}", Long.MAX_VALUE))
						.andExpect(status().isNotFound());
	}

	@Test
	public void updateAsset() throws Exception {
		// Initialize the database
		assetRepository.save(asset);
		int databaseSizeBeforeUpdate = assetRepository	.findAll()
														.size();

		// Update the asset
		Asset updatedAsset = new Asset();
		updatedAsset.setId(asset.getId());
		updatedAsset.setName(UPDATED_NAME);
		AssetDto assetDto = assetMapper.asDto(updatedAsset);

		restAssetMockMvc.perform(put("/api/assets")	.contentType(TestUtil.APPLICATION_JSON_UTF8)
													.content(TestUtil.convertObjectToJsonBytes(assetDto)))
						.andExpect(status().isOk());

		// Validate the Asset in the database
		List<Asset> assets = assetRepository.findAll();
		assertThat(assets).hasSize(databaseSizeBeforeUpdate);
		Asset testAsset = assets.get(assets.size() - 1);
		assertThat(testAsset.getName()).isEqualTo(UPDATED_NAME);
	}

	@Test
	public void deleteAsset() throws Exception {
		// Initialize the database
		assetRepository.save(asset);
		int databaseSizeBeforeDelete = assetRepository	.findAll()
														.size();

		// Get the asset
		restAssetMockMvc.perform(delete("/api/assets/{id}", asset.getId()).accept(TestUtil.APPLICATION_JSON_UTF8))
						.andExpect(status().isOk());

		// Validate the database is empty
		List<Asset> assets = assetRepository.findAll();
		assertThat(assets).hasSize(databaseSizeBeforeDelete - 1);
	}
}
