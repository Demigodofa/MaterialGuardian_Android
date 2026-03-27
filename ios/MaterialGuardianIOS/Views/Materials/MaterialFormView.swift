import SwiftUI
import UIKit
import VisionKit

struct MaterialFormView: View {
    @Environment(\.dismiss) private var dismiss

    @State private var material: MaterialRecord
    @State private var hasLoadedDraft = false
    @State private var didSave = false
    @State private var showingExitConfirm = false
    @State private var showingSaveSuccess = false
    @State private var saveError: String?
    @State private var showingMaxPhotos = false
    @State private var showingScanLimit = false
    @State private var showingScannerOptions = false
    @State private var showingScannerUnavailable = false
    @State private var activePhotoRequest: PhotoCaptureRequest?
    @State private var activeScanRequest: ScanCaptureRequest?
    @State private var pendingReview: PendingReview?
    @State private var activeMediaMenu: ActiveMediaMenu?
    @State private var activeSignatureTarget: SignatureTarget?

    private let draftStore = MaterialDraftStore.shared
    private let mediaStore = MaterialMediaStore()
    private let signatureStore = SignatureAssetStore()
    let draftKey: String
    let onSave: (MaterialRecord) -> Void

    init(material: MaterialRecord, draftKey: String, onSave: @escaping (MaterialRecord) -> Void) {
        _material = State(initialValue: material)
        self.draftKey = draftKey
        self.onSave = onSave
    }

    var body: some View {
        applyDialogModifiers(
            to: applyLifecycleModifiers(
                to: applyPresentationModifiers(
                    to: formContent
                )
            )
        )
    }

    private var formContent: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                MGHeaderBar(onBack: { showingExitConfirm = true })

                Text("RECEIVING INSPECTION\nREPORT")
                    .font(.system(size: 26, weight: .bold))
                    .multilineTextAlignment(.center)
                    .foregroundStyle(Brand.sectionTitle)
                    .frame(maxWidth: .infinity)

                receivingSection
                productSection
                inspectionSection
                qcSection
                photosSection
                scansSection

                HStack(spacing: 12) {
                    Button("Save Material", action: saveMaterial)
                        .buttonStyle(FilledFormButtonStyle(color: Brand.primaryButton, textColor: Brand.primaryButtonText))

                    Button("Cancel") {
                        showingExitConfirm = true
                    }
                    .buttonStyle(FilledFormButtonStyle(color: Brand.editButton, textColor: Brand.editButtonText))
                }
            }
            .padding(20)
        }
    }

    private func applyPresentationModifiers<Content: View>(to content: Content) -> some View {
        content
            .background(Brand.formBackground.ignoresSafeArea())
            .navigationBarBackButtonHidden(true)
            .toolbar(.hidden, for: .navigationBar)
    }

    private func applyLifecycleModifiers<Content: View>(to content: Content) -> some View {
        content
            .task(loadDraftIfNeeded)
            .onChange(of: material) { oldValue, newValue in
                handleMaterialChange(oldValue, newValue)
            }
            .onDisappear(perform: handleDisappear)
    }

    private func applyDialogModifiers<Content: View>(to content: Content) -> some View {
        let alertsApplied = applyAlertModifiers(to: content)
        let dialogsApplied = applyConfirmationDialogModifiers(to: alertsApplied)
        return applySheetModifiers(to: dialogsApplied)
    }

    private func applyAlertModifiers<Content: View>(to content: Content) -> some View {
        content
            .alert("Exit receiving report?", isPresented: $showingExitConfirm) {
            Button("Keep Draft") {
                draftStore.saveDraft(material, for: draftKey)
                dismiss()
            }
            Button("Keep Editing", role: .cancel) {}
            Button("Delete Draft", role: .destructive) {
                draftStore.clearDraft(for: draftKey)
                dismiss()
            }
        } message: {
            Text("This report is autosaved as a draft. Leave now and keep the draft, or delete it.")
        }
        .alert("Material saved", isPresented: $showingSaveSuccess) {
            Button("OK") {
                dismiss()
            }
        } message: {
            Text("This material entry was saved to the job.")
        }
        .alert("Save failed", isPresented: saveErrorBinding) {
            Button("OK", role: .cancel) {
                saveError = nil
            }
        } message: {
            Text(saveErrorMessage)
        }
        .alert("Max photos taken", isPresented: $showingMaxPhotos) {
            Button("OK", role: .cancel) {}
        } message: {
            Text("You have reached the 4-photo limit for this material.")
        }
        .alert("Scan limit reached", isPresented: $showingScanLimit) {
            Button("OK", role: .cancel) {}
        } message: {
            Text("You can attach up to 8 scans per material.")
        }
        .alert("Scanner unavailable", isPresented: $showingScannerUnavailable) {
            Button("OK", role: .cancel) {}
        } message: {
            Text("Using camera capture instead of the document scanner. These pages will still be bundled into the exported MTR PDF.")
        }
    }

    private func applyConfirmationDialogModifiers<Content: View>(to content: Content) -> some View {
        content
        .alert(reviewTitle, isPresented: pendingReviewBinding, presenting: pendingReview) { pending in
            Button("Keep") {
                commitPendingReview(pending)
            }
            Button("Retake") {
                retakePendingReview(pending)
            }
            Button("Exit", role: .cancel) {
                pendingReview = nil
            }
        } message: { pending in
            Text(pending.message)
        }
        .confirmationDialog(
            "Add material photo",
            isPresented: choosePhotoSourceBinding,
            titleVisibility: .visible
        ) {
            Button("Take Photo") {
                activePhotoRequest = PhotoCaptureRequest(kind: .photo, source: .camera, replacingIndex: nil)
            }
            Button("Choose Existing Photo") {
                activePhotoRequest = PhotoCaptureRequest(kind: .photo, source: .photoLibrary, replacingIndex: nil)
            }
            Button("Cancel", role: .cancel) {
                activePhotoRequest = nil
            }
        } message: {
            Text("Capture a new jobsite image or attach one already on this device.")
        }
        .confirmationDialog(
            "Media options",
            isPresented: activeMediaMenuBinding,
            titleVisibility: .visible
        ) {
            Button("Retake") {
                retakeActiveMedia()
            }
            Button("Delete", role: .destructive) {
                deleteActiveMedia()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Would you like to retake or delete this file?")
        }
        .confirmationDialog(
            "Scan MTR/CoC PDFs",
            isPresented: $showingScannerOptions,
            titleVisibility: .visible
        ) {
            Button("Use Document Scanner") {
                if VNDocumentCameraViewController.isSupported {
                    activeScanRequest = ScanCaptureRequest(replacingIndex: nil)
                } else {
                    showingScannerUnavailable = true
                    activePhotoRequest = PhotoCaptureRequest(kind: .scanFallback, source: .camera, replacingIndex: nil)
                }
            }
            Button("Use Camera Fallback") {
                showingScannerUnavailable = true
                activePhotoRequest = PhotoCaptureRequest(kind: .scanFallback, source: .camera, replacingIndex: nil)
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Preferred: document scanner. Camera fallback still exports cleanly into the combined MTR PDF.")
        }
    }

    private func applySheetModifiers<Content: View>(to content: Content) -> some View {
        content
        .sheet(item: $activePhotoRequest) { request in
            if let source = request.source {
                ImagePickerSheet(source: source) { image in
                    handlePickedImage(image, request: request)
                    activePhotoRequest = nil
                } onCancel: {
                    activePhotoRequest = nil
                }
            }
        }
        .sheet(item: $activeScanRequest) { request in
            DocumentScannerSheet { scan in
                handleDocumentScan(scan, request: request)
                activeScanRequest = nil
            } onCancel: {
                activeScanRequest = nil
            } onFailure: { _ in
                activeScanRequest = nil
                showingScannerUnavailable = true
                activePhotoRequest = PhotoCaptureRequest(kind: .scanFallback, source: .camera, replacingIndex: request.replacingIndex)
            }
        }
        .sheet(item: $activeSignatureTarget) { target in
            SignatureCaptureSheet(title: target.title, existing: signature(for: target)) { signature in
                saveSignature(signature, for: target)
                activeSignatureTarget = nil
            } onCancel: {
                activeSignatureTarget = nil
            }
        }
    }

    private var receivingSection: some View {
        formSection(title: "Receiving") {
            field("Material description", text: $material.description)

            HStack(spacing: 12) {
                field("PO #", text: $material.poNumber)
                field("Vendor", text: $material.vendor)
            }

            HStack(spacing: 12) {
                field("Qty", text: $material.quantity, keyboard: .numbersAndPunctuation)
                menuField("Product", selection: $material.productType, options: ["Tube", "Pipe", "Plate", "Fitting", "Bar", "Other"])
                menuField("A/SA", selection: $material.specificationPrefix, options: ["A", "SA"], width: 82)
            }

            DatePicker("Received at", selection: $material.receivedAt, displayedComponents: .date)
                .datePickerStyle(.compact)
                .foregroundStyle(Brand.textPrimary)
        }
    }

    private var productSection: some View {
        formSection(title: "Product / Specification") {
            HStack(spacing: 12) {
                field("Spec/Grade", text: $material.gradeType)
                menuField("Fitting", selection: $material.fittingStandard, options: ["N/A", "B16"])
                menuField("", selection: $material.fittingSuffix, options: ["", "5", "9", "11", "34"], labelForOption: { $0.isEmpty ? "Clear" : $0 }, disabled: material.fittingStandard != "B16", width: 84)
            }

            LabeledField(title: "Dimensions") {
                HStack(spacing: 14) {
                    xToggle("Imperial", selected: material.dimensionUnit == .imperial) { material.dimensionUnit = .imperial }
                    xToggle("Metric", selected: material.dimensionUnit == .metric) { material.dimensionUnit = .metric }
                }
            }

            HStack(spacing: 12) {
                field("TH 1", text: $material.thickness1, keyboard: .numbersAndPunctuation)
                field("TH 2", text: $material.thickness2, keyboard: .numbersAndPunctuation)
                field("TH 3", text: $material.thickness3, keyboard: .numbersAndPunctuation)
                field("TH 4", text: $material.thickness4, keyboard: .numbersAndPunctuation)
            }

            HStack(spacing: 12) {
                field("Width", text: $material.width, keyboard: .numbersAndPunctuation)
                field("Length", text: $material.length, keyboard: .numbersAndPunctuation)
                field("Diameter", text: $material.diameter, keyboard: .numbersAndPunctuation)
                menuField("ID/OD", selection: $material.diameterType, options: ["", "O.D.", "I.D."], labelForOption: { $0.isEmpty ? "Clear" : $0 }, width: 88)
            }

            HStack(spacing: 12) {
                yesNoField(
                    title: "Visual inspection acceptable",
                    isYes: Binding(
                        get: { material.visualInspectionAcceptable },
                        set: { material.visualInspectionAcceptable = $0 }
                    )
                )
                menuField("B16 Dimensions", selection: $material.b16DimensionsAcceptable, options: ["", "Yes", "No"], labelForOption: { $0.isEmpty ? "Clear" : $0 })
            }

            field("Specification numbers", text: $material.specificationNumbers, axis: .vertical)
            field("Actual markings", text: $material.markings, axis: .vertical)
        }
    }

    private var inspectionSection: some View {
        formSection(title: "Inspection / Disposition") {
            yesNoNaField(
                title: "Marking acceptable to Code/Standard",
                isYes: Binding(
                    get: { material.markingAcceptable && !material.markingAcceptableNa },
                    set: { selected in
                        material.markingAcceptable = selected
                        material.markingAcceptableNa = false
                    }
                ),
                isNo: Binding(
                    get: { !material.markingAcceptable && !material.markingAcceptableNa },
                    set: { selected in
                        if selected {
                            material.markingAcceptable = false
                            material.markingAcceptableNa = false
                        }
                    }
                ),
                isNa: Binding(
                    get: { material.markingAcceptableNa },
                    set: { selected in
                        material.markingAcceptableNa = selected
                        if selected {
                            material.markingAcceptable = false
                        }
                    }
                )
            )

            yesNoNaField(
                title: "MTR/CoC acceptable to specification",
                isYes: Binding(
                    get: { material.mtrAcceptable && !material.mtrAcceptableNa },
                    set: { selected in
                        material.mtrAcceptable = selected
                        material.mtrAcceptableNa = false
                    }
                ),
                isNo: Binding(
                    get: { !material.mtrAcceptable && !material.mtrAcceptableNa },
                    set: { selected in
                        if selected {
                            material.mtrAcceptable = false
                            material.mtrAcceptableNa = false
                        }
                    }
                ),
                isNa: Binding(
                    get: { material.mtrAcceptableNa },
                    set: { selected in
                        material.mtrAcceptableNa = selected
                        if selected {
                            material.mtrAcceptable = false
                        }
                    }
                )
            )

            LabeledField(title: "Disposition") {
                HStack(spacing: 14) {
                    xToggle("Accept", selected: material.acceptanceStatus == .accept) { material.acceptanceStatus = .accept }
                    xToggle("Hold", selected: material.acceptanceStatus == .hold) { material.acceptanceStatus = .hold }
                    xToggle("Reject", selected: material.acceptanceStatus == .reject) { material.acceptanceStatus = .reject }
                }
            }
        }
    }

    private var qcSection: some View {
        formSection(title: "Quality Control") {
            HStack(spacing: 12) {
                field("QC initials", text: $material.qcInitials)
                    .textInputAutocapitalization(.characters)
                DatePicker("QC date", selection: $material.qcDate, displayedComponents: .date)
                    .datePickerStyle(.compact)
            }

            signatureField(
                title: "QC inspector signature",
                signature: material.qcInspectorSignature,
                onSign: { activeSignatureTarget = .qcInspector },
                onClear: { clearSignature(for: .qcInspector) }
            )

            LabeledField(title: "Material approval") {
                HStack(spacing: 14) {
                    xToggle("Approved", selected: material.materialApproval == .approved) { material.materialApproval = .approved }
                    xToggle("Review", selected: material.materialApproval == .reviewRequired) { material.materialApproval = .reviewRequired }
                    xToggle("Rejected", selected: material.materialApproval == .rejected) { material.materialApproval = .rejected }
                }
            }

            HStack(spacing: 12) {
                field("QC manager", text: $material.qcManager)
                field("Manager initials", text: $material.qcManagerInitials)
                    .textInputAutocapitalization(.characters)
            }

            DatePicker("QC manager date", selection: $material.qcManagerDate, displayedComponents: .date)
                .datePickerStyle(.compact)

            signatureField(
                title: "QC manager signature",
                signature: material.qcManagerSignature,
                onSign: { activeSignatureTarget = .qcManager },
                onClear: { clearSignature(for: .qcManager) }
            )

            field("Comments", text: $material.comments, axis: .vertical)
        }
    }

    private var photosSection: some View {
        formSection(title: "Material photos") {
            Button {
                startPhoto()
            } label: {
                Text("Add material photos (\(material.photoCount)/4)")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(OutlinedMediaButtonStyle())

            Text("Use these for arrival condition, markings, and visible damage.")
                .font(.footnote)
                .foregroundStyle(Brand.textSecondary)

            MediaThumbnailRow(attachments: material.photoAttachments, maxCount: 4, symbolName: "camera") { index in
                activeMediaMenu = ActiveMediaMenu(kind: .photo, index: index)
            }
        }
    }

    private var scansSection: some View {
        formSection(title: "MTR/CoC scans") {
            Button {
                if material.scanCount >= 8 {
                    showingScanLimit = true
                } else {
                    showingScannerOptions = true
                }
            } label: {
                Text("Scan MTR/CoC PDFs (\(material.scanCount)/8)")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(OutlinedMediaButtonStyle())

            Text("Preferred: document scanner. Camera fallback still exports cleanly into the combined MTR PDF.")
                .font(.footnote)
                .foregroundStyle(Brand.textSecondary)

            MediaThumbnailRow(attachments: material.scanAttachments, maxCount: 8, symbolName: "doc.viewfinder") { index in
                activeMediaMenu = ActiveMediaMenu(kind: .scan, index: index)
            }
        }
    }

    private var saveErrorBinding: Binding<Bool> {
        Binding(
            get: { saveError != nil },
            set: { if !$0 { saveError = nil } }
        )
    }

    private var pendingReviewBinding: Binding<Bool> {
        Binding(
            get: { pendingReview != nil },
            set: { if !$0 { pendingReview = nil } }
        )
    }

    private var activeMediaMenuBinding: Binding<Bool> {
        Binding(
            get: { activeMediaMenu != nil },
            set: { if !$0 { activeMediaMenu = nil } }
        )
    }

    private var choosePhotoSourceBinding: Binding<Bool> {
        Binding(
            get: { activePhotoRequest?.kind == .photo && activePhotoRequest?.source == nil },
            set: { if !$0, activePhotoRequest?.source == nil { activePhotoRequest = nil } }
        )
    }

    private var reviewTitle: String {
        guard let pendingReview else { return "Review file" }
        return pendingReview.kind == .photo ? "Review photo" : "Review scan"
    }

    private var saveErrorMessage: String {
        saveError ?? "Unable to save material."
    }

    private func saveMaterial() {
        let trimmedDescription = material.description.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedDescription.isEmpty else {
            saveError = "Material description is required before saving."
            return
        }

        material.description = trimmedDescription
        didSave = true
        draftStore.clearDraft(for: draftKey)
        onSave(material)
        showingSaveSuccess = true
    }

    private func handleDisappear() {
        guard hasLoadedDraft else { return }
        if didSave {
            draftStore.clearDraft(for: draftKey)
            return
        }
        persistCurrentDraft()
    }

    @MainActor
    private func persistCurrentDraft() {
        draftStore.saveDraft(material, for: draftKey)
    }

    @MainActor
    private func loadDraftIfNeeded() {
        guard !hasLoadedDraft else { return }
        if let draft = draftStore.loadDraft(for: draftKey) {
            material = draft
        }
        hasLoadedDraft = true
    }

    @MainActor
    private func handleMaterialChange(_ oldValue: MaterialRecord, _ updated: MaterialRecord) {
        guard hasLoadedDraft, !didSave else { return }
        draftStore.saveDraft(updated, for: draftKey)
    }

    private func startPhoto(replacing index: Int? = nil) {
        guard index != nil || material.photoCount < 4 else {
            showingMaxPhotos = true
            return
        }
        if index == nil {
            activePhotoRequest = PhotoCaptureRequest(kind: .photo, source: nil, replacingIndex: nil)
        } else {
            activePhotoRequest = PhotoCaptureRequest(kind: .photo, source: .camera, replacingIndex: index)
        }
    }

    private func startScan(source: MediaAttachment.Source, replacing index: Int? = nil) {
        guard index != nil || material.scanCount < 8 else {
            showingScanLimit = true
            return
        }
        switch source {
        case .scan:
            if VNDocumentCameraViewController.isSupported {
                activeScanRequest = ScanCaptureRequest(replacingIndex: index)
            } else {
                showingScannerUnavailable = true
                activePhotoRequest = PhotoCaptureRequest(kind: .scanFallback, source: .camera, replacingIndex: index)
            }
        case .cameraFallback:
            activePhotoRequest = PhotoCaptureRequest(kind: .scanFallback, source: .camera, replacingIndex: index)
        case .photo:
            break
        }
    }

    private func scanLabelIndex(_ replacing: Int?) -> Int {
        (replacing ?? material.scanCount) + 1
    }

    private func commitPendingReview(_ pending: PendingReview) {
        let storedAttachment: MediaAttachment
        do {
            storedAttachment = try mediaStore.persist(media: pending.storedMedia, for: pending.attachment)
        } catch {
            saveError = "Unable to save this file on the device."
            pendingReview = nil
            return
        }

        switch pending.kind {
        case .photo:
            if let index = pending.replaceIndex, material.photoAttachments.indices.contains(index) {
                mediaStore.deleteFile(for: material.photoAttachments[index])
                material.photoAttachments[index] = storedAttachment
            } else {
                material.photoAttachments.append(storedAttachment)
            }
            if material.photoCount >= 4 {
                showingMaxPhotos = true
            }
        case .scan:
            if let index = pending.replaceIndex, material.scanAttachments.indices.contains(index) {
                mediaStore.deleteFile(for: material.scanAttachments[index])
                material.scanAttachments[index] = storedAttachment
            } else {
                material.scanAttachments.append(storedAttachment)
            }
            if material.scanCount >= 8 {
                showingScanLimit = true
            }
        }
        pendingReview = nil
    }

    private func retakePendingReview(_ pending: PendingReview) {
        pendingReview = nil
        switch pending.retakeAction {
        case .photo(let source, let replacingIndex):
            activePhotoRequest = PhotoCaptureRequest(kind: .photo, source: source, replacingIndex: replacingIndex)
        case .scanFallback(let source, let replacingIndex):
            activePhotoRequest = PhotoCaptureRequest(kind: .scanFallback, source: source, replacingIndex: replacingIndex)
        case .documentScan(let replacingIndex):
            activeScanRequest = ScanCaptureRequest(replacingIndex: replacingIndex)
        }
    }

    private func retakeActiveMedia() {
        guard let activeMediaMenu else { return }
        let kind = activeMediaMenu.kind
        let index = activeMediaMenu.index
        self.activeMediaMenu = nil
        switch kind {
        case .photo:
            startPhoto(replacing: index)
        case .scan:
            startScan(source: material.scanAttachments[index].source, replacing: index)
        }
    }

    private func deleteActiveMedia() {
        guard let activeMediaMenu else { return }
        switch activeMediaMenu.kind {
        case .photo:
            guard material.photoAttachments.indices.contains(activeMediaMenu.index) else { break }
            mediaStore.deleteFile(for: material.photoAttachments[activeMediaMenu.index])
            material.photoAttachments.remove(at: activeMediaMenu.index)
        case .scan:
            guard material.scanAttachments.indices.contains(activeMediaMenu.index) else { break }
            mediaStore.deleteFile(for: material.scanAttachments[activeMediaMenu.index])
            material.scanAttachments.remove(at: activeMediaMenu.index)
        }
        self.activeMediaMenu = nil
    }

    private func handlePickedImage(_ image: UIImage, request: PhotoCaptureRequest) {
        guard let imageData = image.jpegData(compressionQuality: 0.9) else {
            saveError = "Unable to prepare the selected image."
            return
        }

        switch request.kind {
        case .photo:
            pendingReview = PendingReview(
                attachment: MediaAttachment(
                    id: UUID(),
                    label: request.replacingIndex == nil ? "Photo \(material.photoCount + 1)" : "Photo \((request.replacingIndex ?? 0) + 1)",
                    createdAt: .now,
                    source: .photo,
                    relativeFilePath: nil
                ),
                kind: .photo,
                replaceIndex: request.replacingIndex,
                storedMedia: .jpeg(imageData),
                retakeAction: .photo(source: request.source ?? .camera, replacingIndex: request.replacingIndex)
            )
        case .scanFallback:
            pendingReview = PendingReview(
                attachment: MediaAttachment(
                    id: UUID(),
                    label: "Camera fallback scan \(scanLabelIndex(request.replacingIndex))",
                    createdAt: .now,
                    source: .cameraFallback,
                    relativeFilePath: nil
                ),
                kind: .scan,
                replaceIndex: request.replacingIndex,
                storedMedia: .jpeg(imageData),
                retakeAction: .scanFallback(source: request.source ?? .camera, replacingIndex: request.replacingIndex)
            )
        }
    }

    private func handleDocumentScan(_ scan: VNDocumentCameraScan, request: ScanCaptureRequest) {
        pendingReview = PendingReview(
            attachment: MediaAttachment(
                id: UUID(),
                label: "Scan \(scanLabelIndex(request.replacingIndex))",
                createdAt: .now,
                source: .scan,
                relativeFilePath: nil
            ),
            kind: .scan,
            replaceIndex: request.replacingIndex,
            storedMedia: .pdf(pdfData(from: scan)),
            retakeAction: .documentScan(replacingIndex: request.replacingIndex)
        )
    }

    private func pdfData(from scan: VNDocumentCameraScan) -> Data {
        let pageBounds = CGRect(x: 0, y: 0, width: 612, height: 792)
        let renderer = UIGraphicsPDFRenderer(bounds: pageBounds)
        return renderer.pdfData { context in
            for index in 0..<scan.pageCount {
                let image = scan.imageOfPage(at: index)
                context.beginPage()
                image.draw(in: aspectFitRect(for: image.size, inside: pageBounds.insetBy(dx: 24, dy: 24)))
            }
        }
    }

    private func aspectFitRect(for size: CGSize, inside rect: CGRect) -> CGRect {
        guard size.width > 0, size.height > 0 else { return rect }
        let scale = min(rect.width / size.width, rect.height / size.height)
        let fittedSize = CGSize(width: size.width * scale, height: size.height * scale)
        return CGRect(
            x: rect.midX - fittedSize.width / 2,
            y: rect.midY - fittedSize.height / 2,
            width: fittedSize.width,
            height: fittedSize.height
        )
    }

    private func signature(for target: SignatureTarget) -> SignatureCapture? {
        switch target {
        case .qcInspector:
            material.qcInspectorSignature
        case .qcManager:
            material.qcManagerSignature
        }
    }

    private func saveSignature(_ signature: SignatureCapture, for target: SignatureTarget) {
        let existingSignature = self.signature(for: target)
        do {
            let stored = try signatureStore.persist(signature: signature)
            signatureStore.deleteFile(for: existingSignature)
            setSignature(stored, for: target)
        } catch {
            saveError = "Unable to save this signature on the device."
        }
    }

    private func clearSignature(for target: SignatureTarget) {
        signatureStore.deleteFile(for: signature(for: target))
        setSignature(nil, for: target)
    }

    private func setSignature(_ signature: SignatureCapture?, for target: SignatureTarget) {
        switch target {
        case .qcInspector:
            material.qcInspectorSignature = signature
        case .qcManager:
            material.qcManagerSignature = signature
        }
    }

    private func formSection<Content: View>(title: String, @ViewBuilder content: () -> Content) -> some View {
        MGCard {
            Text(title)
                .font(.headline)
                .foregroundStyle(Brand.sectionTitle)
            VStack(alignment: .leading, spacing: 14) {
                content()
            }
        }
    }

    private func field(
        _ title: String,
        text: Binding<String>,
        axis: Axis = .horizontal,
        keyboard: UIKeyboardType = .default
    ) -> some View {
        LabeledField(title: title) {
            TextField(title, text: text, axis: axis)
                .keyboardType(keyboard)
                .padding(.horizontal, 12)
                .padding(.vertical, axis == .horizontal ? 12 : 14)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.white)
                .overlay(
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .stroke(Brand.border, lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        }
    }

    private func menuField(
        _ title: String,
        selection: Binding<String>,
        options: [String],
        labelForOption: @escaping (String) -> String = { $0 },
        disabled: Bool = false,
        width: CGFloat? = nil
    ) -> some View {
        LabeledField(title: title) {
            Menu {
                ForEach(options, id: \.self) { option in
                    Button(labelForOption(option)) {
                        selection.wrappedValue = option
                    }
                }
            } label: {
                HStack {
                    Text(selection.wrappedValue.isEmpty ? "Select" : labelForOption(selection.wrappedValue))
                        .foregroundStyle(selection.wrappedValue.isEmpty ? Brand.textMuted : Brand.textPrimary)
                    Spacer()
                    Image(systemName: "chevron.down")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(Brand.textSecondary)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 13)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.white)
                .overlay(
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .stroke(Brand.border, lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                .frame(width: width)
            }
            .disabled(disabled)
            .opacity(disabled ? 0.55 : 1)
        }
    }

    private func yesNoField(title: String, isYes: Binding<Bool>) -> some View {
        LabeledField(title: title) {
            HStack(spacing: 14) {
                xToggle("Yes", selected: isYes.wrappedValue) { isYes.wrappedValue = true }
                xToggle("No", selected: !isYes.wrappedValue) { isYes.wrappedValue = false }
            }
        }
    }

    private func yesNoNaField(title: String, isYes: Binding<Bool>, isNo: Binding<Bool>, isNa: Binding<Bool>) -> some View {
        LabeledField(title: title) {
            HStack(spacing: 14) {
                xToggle("Yes", selected: isYes.wrappedValue) {
                    isYes.wrappedValue = true
                    isNo.wrappedValue = false
                    isNa.wrappedValue = false
                }
                xToggle("No", selected: isNo.wrappedValue) {
                    isYes.wrappedValue = false
                    isNo.wrappedValue = true
                    isNa.wrappedValue = false
                }
                xToggle("N/A", selected: isNa.wrappedValue) {
                    isYes.wrappedValue = false
                    isNo.wrappedValue = false
                    isNa.wrappedValue = true
                }
            }
        }
    }

    private func xToggle(_ label: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 6) {
                ZStack {
                    RoundedRectangle(cornerRadius: 4)
                        .stroke(Brand.textSecondary, lineWidth: 1)
                        .frame(width: 22, height: 22)
                    if selected {
                        Text("X")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundStyle(Brand.textPrimary)
                    }
                }
                Text(label)
                    .foregroundStyle(Brand.textPrimary)
            }
        }
        .buttonStyle(.plain)
    }

    private func signatureField(title: String, signature: SignatureCapture?, onSign: @escaping () -> Void, onClear: @escaping () -> Void) -> some View {
        LabeledField(title: title) {
            VStack(alignment: .leading, spacing: 10) {
                HStack(spacing: 12) {
                    Button(signature == nil ? "Capture Signature" : "Re-sign", action: onSign)
                        .buttonStyle(OutlinedMediaButtonStyle())
                    if signature != nil {
                        Button("Clear", role: .destructive, action: onClear)
                    }
                }

                if let signature {
                    SignaturePreview(strokes: signature.strokes)
                }
            }
        }
    }
}

#Preview {
    NavigationStack {
        MaterialFormView(
            material: .mock(description: "6 in Sch 40 pipe", vendor: "SteelCo", quantity: "12"),
            draftKey: "preview-job-new"
        ) { _ in }
    }
}
