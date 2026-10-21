package ukma.jpay.payments.web.page;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ukma.jpay.payments.domain.Merchant;
import ukma.jpay.payments.domain.PaymentProvider;
import ukma.jpay.payments.error.DuplicateMerchantEmailException;
import ukma.jpay.payments.service.MerchantService;
import ukma.jpay.payments.service.ProviderService;

import java.util.List;

@Controller
@RequestMapping("/merchants")
public class MerchantPageController {

    private final MerchantService merchantService;
    private final ProviderService providerService;

    public MerchantPageController(MerchantService merchantService, ProviderService providerService) {
        this.merchantService = merchantService;
        this.providerService = providerService;
    }

    @ModelAttribute("providers")
    public List<PaymentProvider> providers() {
        return providerService.list();
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("merchants", merchantService.list());
        return "merchants/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("merchantForm", new MerchantForm());
        return "merchants/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("merchantForm") MerchantForm form,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "merchants/form";
        }
        try {
            Merchant merchant = merchantService.create(form.getName(), form.getContactEmail(),
                    form.getDefaultCurrency(), form.getProviderIds());
            redirectAttributes.addFlashAttribute("message", "Merchant \"" + merchant.name() + "\" created");
        } catch (DuplicateMerchantEmailException exception) {
            bindingResult.rejectValue("contactEmail", "duplicate", "is already used by another merchant");
            return "merchants/form";
        }
        return "redirect:/merchants";
    }
}
