package com.ableneo.liferay.portal.setup.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.ableneo.liferay.portal.setup.SetupConfigurationThreadLocal;
import com.ableneo.liferay.portal.setup.ValidSetupTestMocks;
import com.ableneo.liferay.portal.setup.domain.PageType;
import com.ableneo.liferay.portal.setup.domain.PagesType;
import com.ableneo.liferay.portal.setup.domain.Site;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.service.LayoutLocalServiceUtil;
import com.liferay.portal.kernel.util.UnicodeProperties;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SetupPagesTest extends ValidSetupTestMocks {

    private static final long GROUP_ID = 20L;
    private static final long PLID = 100L;
    private static final long SECOND_PLID = 101L;
    private static final long THIRD_PLID = 102L;

    private MockedStatic<LayoutLocalServiceUtil> layoutLocalServiceUtilMockedStatic;

    @Mock(lenient = true)
    private Layout layout;

    @Mock(lenient = true)
    private Layout secondLayout;

    @Mock(lenient = true)
    private Layout thirdLayout;

    @BeforeEach
    void setUpLayoutMocks() throws PortalException {
        layoutLocalServiceUtilMockedStatic = Mockito.mockStatic(LayoutLocalServiceUtil.class);
        when(layout.getPlid()).thenReturn(PLID);
        when(layout.getTypeSettingsProperties()).thenReturn(new UnicodeProperties());
        when(secondLayout.getPlid()).thenReturn(SECOND_PLID);
        when(secondLayout.getTypeSettingsProperties()).thenReturn(new UnicodeProperties());
        when(thirdLayout.getPlid()).thenReturn(THIRD_PLID);
        when(thirdLayout.getTypeSettingsProperties()).thenReturn(new UnicodeProperties());
        layoutLocalServiceUtilMockedStatic
            .when(() -> LayoutLocalServiceUtil.getFriendlyURLLayout(anyLong(), anyBoolean(), anyString()))
            .thenReturn(layout);
        setupConfigurationThreadLocalMockedStatic.when(SetupConfigurationThreadLocal::getRunInCompanyId).thenReturn(1L);
        setupConfigurationThreadLocalMockedStatic.when(SetupConfigurationThreadLocal::getRunAsUserId).thenReturn(1L);
    }

    @AfterEach
    void tearDownLayoutMocks() {
        layoutLocalServiceUtilMockedStatic.close();
    }

    @Test
    void shouldUpdatePriorityWithConfiguredValueWhenPageSequenceIsSet() throws PortalException {
        SetupPages.setupSitePages(siteWithPublicPages(page("/news", BigInteger.valueOf(2))), GROUP_ID);

        layoutLocalServiceUtilMockedStatic.verify(() -> LayoutLocalServiceUtil.updatePriority(PLID, 2), times(1));
        // the raw configured page sequence is passed, the priority buffer is Liferay's business
        layoutLocalServiceUtilMockedStatic.verify(() -> LayoutLocalServiceUtil.updatePriority(PLID, 1000002), never());
    }

    @Test
    void shouldUpdatePriorityWithZeroWhenPageSequenceIsZero() throws PortalException {
        SetupPages.setupSitePages(siteWithPublicPages(page("/home", BigInteger.ZERO)), GROUP_ID);

        layoutLocalServiceUtilMockedStatic.verify(() -> LayoutLocalServiceUtil.updatePriority(PLID, 0), times(1));
    }

    @Test
    void shouldNotUpdatePriorityWhenPageSequenceIsAbsent() throws PortalException {
        SetupPages.setupSitePages(siteWithPublicPages(page("/home", null)), GROUP_ID);

        layoutLocalServiceUtilMockedStatic.verify(
            () -> LayoutLocalServiceUtil.updatePriority(anyLong(), anyInt()),
            never()
        );
    }

    @Test
    void shouldSetupRemainingPagesWhenUpdatePriorityFails() throws PortalException {
        layoutLocalServiceUtilMockedStatic
            .when(() -> LayoutLocalServiceUtil.getFriendlyURLLayout(anyLong(), anyBoolean(), anyString()))
            .thenReturn(layout, secondLayout);
        layoutLocalServiceUtilMockedStatic
            .when(() -> LayoutLocalServiceUtil.updatePriority(PLID, 1))
            .thenThrow(new PortalException("page is not sortable"));

        Site site = siteWithPublicPages(page("/home", BigInteger.ONE), page("/news", BigInteger.valueOf(3)));

        assertDoesNotThrow(() -> SetupPages.setupSitePages(site, GROUP_ID));

        layoutLocalServiceUtilMockedStatic.verify(() -> LayoutLocalServiceUtil.updatePriority(PLID, 1), times(1));
        layoutLocalServiceUtilMockedStatic.verify(
            () -> LayoutLocalServiceUtil.updatePriority(SECOND_PLID, 3),
            times(1)
        );
    }

    /**
     * Pins both halves of the ordering contract in one assertion: no page is moved
     * before the whole sibling group has been looked up or created, and the moves are
     * then applied in ascending page-sequence order instead of document order.
     * <p>
     * Applying them in document order gives an insertion sort, where each move shifts
     * the pages already placed. For the declaration below that produced
     * /first, /third, /second in a real portal.
     */
    @Test
    void shouldApplyPageSequencesInAscendingOrderAfterWholeSiblingGroupExists() throws PortalException {
        List<String> calls = recordLayoutCalls();

        SetupPages.setupSitePages(
            siteWithPublicPages(
                page("/third", BigInteger.valueOf(2)),
                page("/second", BigInteger.ONE),
                page("/first", BigInteger.ZERO)
            ),
            GROUP_ID
        );

        assertEquals(
            List.of(
                "lookup /third",
                "lookup /second",
                "lookup /first",
                "priority " + PLID + "=0",
                "priority " + SECOND_PLID + "=1",
                "priority " + THIRD_PLID + "=2"
            ),
            calls
        );
    }

    @Test
    void shouldNotMovePagesWithoutPageSequenceWhenSiblingsAreSequenced() throws PortalException {
        List<String> calls = recordLayoutCalls();

        SetupPages.setupSitePages(siteWithPublicPages(page("/second", BigInteger.ONE), page("/first", null)), GROUP_ID);

        assertEquals(List.of("lookup /second", "lookup /first", "priority " + SECOND_PLID + "=1"), calls);
    }

    /**
     * Records the order of the layout lookups and the priority updates. Resolving the
     * layout by friendly URL keeps the expected values in the tests readable, unlike
     * consecutive thenReturn stubbing.
     */
    private List<String> recordLayoutCalls() {
        Map<String, Layout> layoutsByFriendlyUrl = new HashMap<>();
        layoutsByFriendlyUrl.put("/first", layout);
        layoutsByFriendlyUrl.put("/second", secondLayout);
        layoutsByFriendlyUrl.put("/third", thirdLayout);

        List<String> calls = new ArrayList<>();
        layoutLocalServiceUtilMockedStatic
            .when(() -> LayoutLocalServiceUtil.getFriendlyURLLayout(anyLong(), anyBoolean(), anyString()))
            .thenAnswer(invocation -> {
                String friendlyUrl = invocation.getArgument(2);
                calls.add("lookup " + friendlyUrl);
                return layoutsByFriendlyUrl.get(friendlyUrl);
            });
        layoutLocalServiceUtilMockedStatic
            .when(() -> LayoutLocalServiceUtil.updatePriority(anyLong(), anyInt()))
            .thenAnswer(invocation -> {
                calls.add("priority " + invocation.getArgument(0) + "=" + invocation.getArgument(1));
                return null;
            });
        return calls;
    }

    private static Site siteWithPublicPages(PageType... pages) {
        PagesType publicPages = new PagesType();
        publicPages.setLanguageId("en_US");
        for (PageType page : pages) {
            publicPages.getPage().add(page);
        }
        Site site = new Site();
        site.setName("Test Site");
        site.setPublicPages(publicPages);
        return site;
    }

    private static PageType page(String friendlyUrl, BigInteger pageSequence) {
        PageType page = new PageType();
        page.setName(friendlyUrl.substring(1));
        page.setFriendlyUrl(friendlyUrl);
        page.setPageSequence(pageSequence);
        return page;
    }
}
